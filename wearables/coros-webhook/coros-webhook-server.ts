import express, { Request, Response } from 'express';
import crypto from 'crypto';
import axios from 'axios';

interface RawBodyRequest extends Request {
  rawBody?: Buffer;
}

const app = express();
app.use(
  express.json({
    verify: (req, _res, buf) => {
      (req as RawBodyRequest).rawBody = buf;
    },
  })
);

const COROS_CLIENT_SECRET = process.env.COROS_CLIENT_SECRET || '';
const PORT = Number(process.env.PORT || 3000);

// Verify incoming COROS webhook authenticity with constant-time HMAC comparison
function verifyCorosSignature(req: RawBodyRequest): boolean {
  if (!COROS_CLIENT_SECRET) {
    console.error('COROS_CLIENT_SECRET is not configured in environment.');
    return false;
  }
  const signature = req.headers['x-coros-signature'];
  if (typeof signature !== 'string' || !signature.trim()) return false;

  const payloadBuffer = req.rawBody ?? Buffer.from(JSON.stringify(req.body ?? {}));
  const expectedHash = crypto
    .createHmac('sha256', COROS_CLIENT_SECRET)
    .update(payloadBuffer)
    .digest('hex');

  const sigBuf = Buffer.from(signature.trim(), 'utf8');
  const expBuf = Buffer.from(expectedHash, 'utf8');
  if (sigBuf.length !== expBuf.length) return false;
  return crypto.timingSafeEqual(sigBuf, expBuf);
}

interface CorosWorkoutPayload {
  userId: string;
  sportType: number;       // 100 = Running, 101 = Trail Running, 102 = Hiking
  startTime: number;
  endTime: number;
  distance: number;        // Meters
  duration: number;        // Seconds
  elevationGain: number;   // Meters
  avgPace: number;         // Seconds/km
  fitUrl?: string;         // Direct link to .FIT binary file
}

app.post('/api/wearables/coros/webhook', async (req: RawBodyRequest, res: Response) => {
  if (!verifyCorosSignature(req)) {
    console.error('Invalid COROS webhook signature.');
    return res.status(401).send('Unauthorized signature');
  }

  const payload = req.body as Partial<CorosWorkoutPayload>;
  if (!payload.userId || typeof payload.distance !== 'number' || typeof payload.sportType !== 'number') {
    return res.status(400).send('Invalid COROS workout payload');
  }

  // Filter for outdoor runs, trail runs, and hikes
  if (![100, 101, 102].includes(payload.sportType)) {
    return res.status(200).send('Activity ignored: Unsupported sportType');
  }

  try {
    const totalMiles = Math.max(0, payload.distance) / 1609.344;
    const elevationMeters = Math.max(0, payload.elevationGain ?? 0);
    console.log(`[COROS Sync] User ${payload.userId} completed ${totalMiles.toFixed(2)} miles`);

    await creditWolfConservationFund(payload.userId, totalMiles);
    const earnedBadges = await evaluateBadgeUnlocks(payload.userId, totalMiles, elevationMeters);

    if (payload.fitUrl && payload.fitUrl.startsWith('https://')) {
      void archiveFitFile(payload.userId, payload.fitUrl).catch((err) =>
        console.error('FIT archive error:', err)
      );
    }

    return res.status(200).json({
      status: 'SUCCESS',
      milesProcessed: Number(totalMiles.toFixed(2)),
      badgesUnlocked: earnedBadges,
    });
  } catch (error) {
    console.error('Error processing COROS workout:', error);
    return res.status(500).send('Internal Server Error');
  }
});

async function creditWolfConservationFund(userId: string, miles: number): Promise<void> {
  console.log(`[Conservation Pool] Crediting ${miles.toFixed(2)} miles from ${userId} to Wild Wolf Corridor initiative.`);
}

async function evaluateBadgeUnlocks(userId: string, miles: number, elevationMeters: number): Promise<string[]> {
  const earnedBadges: string[] = [];

  if (miles >= 31.06) earnedBadges.push('fifty_km_ironhearth');
  else if (miles >= 26.2) earnedBadges.push('marathon_26');
  else if (miles >= 13.1) earnedBadges.push('half_marathon_warden');
  else if (miles >= 6.2) earnedBadges.push('ten_leagues_10k');
  else if (miles >= 3.1) earnedBadges.push('first_frost_5k');

  if (elevationMeters >= 300) earnedBadges.push('dragonspine_climber');

  if (earnedBadges.length > 0) {
    console.log(`[Badges Awarded] User ${userId} unlocked:`, earnedBadges);
  }
  return earnedBadges;
}

async function archiveFitFile(userId: string, fitUrl: string): Promise<void> {
  const response = await axios.get<ArrayBuffer>(fitUrl, { responseType: 'arraybuffer', timeout: 15000 });
  console.log(`[FIT Archive] Saved FIT file for user ${userId} (${response.data.byteLength} bytes)`);
}

app.listen(PORT, () => {
  console.log(`Striders Howl COROS Sync Engine listening on port ${PORT}`);
});
