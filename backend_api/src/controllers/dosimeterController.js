const ShiftLog = require('../models/ShiftLog');

/**
 * @route   POST /api/dosimeter/sync
 * @desc    Sync batch offline shift logs from Flutter mobile client to Cloud DB
 * @access  Private
 */
exports.syncShiftLogs = async (req, res) => {
  try {
    const { logs } = req.body; // Array of shift log objects

    if (!Array.isArray(logs) || logs.length === 0) {
      return res.status(400).json({ success: false, message: 'Invalid payload: logs array required' });
    }

    const syncedLogs = [];
    for (const item of logs) {
      const existing = await ShiftLog.findOne({ logId: item.logId });
      if (!existing) {
        const newLog = await ShiftLog.create({
          logId: item.logId || `LOG-${Date.now()}-${Math.floor(Math.random() * 1000)}`,
          workerId: item.workerId || req.user.workerId,
          workerName: item.workerName || req.user.name,
          department: item.department || req.user.department,
          bandId: item.bandId || 'BAND-DEMO-01',
          shiftDate: item.shiftDate || new Date().toISOString().split('T')[0],
          startTime: item.startTime || '08:00:00',
          scanTime: item.scanTime || new Date().toTimeString().split(' ')[0],
          durationHours: item.durationHours || 8.0,
          expiryStatus: item.expiryStatus || 'VALID',
          rawDeltaE: item.rawDeltaE || 22.5,
          estimatedDosePpmHr: item.estimatedDosePpmHr || 8.4,
          uncertaintyPpmHr: item.uncertaintyPpmHr || 0.6,
          twa8hrPpm: item.twa8hrPpm || 1.05,
          riskLevel: item.riskLevel || 'SAFE',
          actionRequired: item.actionRequired || 'Normal Operation',
          imageHash: item.imageHash || 'e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855',
          location: item.location || { type: 'Point', coordinates: [77.5946, 12.9716] },
          syncStatus: 'SYNCED'
        });
        syncedLogs.push(newLog);
      } else {
        existing.syncStatus = 'SYNCED';
        await existing.save();
        syncedLogs.push(existing);
      }
    }

    return res.status(200).json({
      success: true,
      message: `Successfully synchronized ${syncedLogs.length} shift scan logs to Cloud Database`,
      syncedCount: syncedLogs.length,
      timestamp: new Date().toISOString()
    });
  } catch (error) {
    return res.status(500).json({ success: false, message: error.message });
  }
};

/**
 * @route   GET /api/dosimeter/logs
 * @desc    Get shift exposure logs with sorting, filtering, & pagination
 * @access  Private
 */
exports.getShiftLogs = async (req, res) => {
  try {
    const page = parseInt(req.query.page) || 1;
    const limit = parseInt(req.query.limit) || 20;
    const skip = (page - 1) * limit;

    const query = {};

    // Filter by Worker ID if passed or if current user is WORKER
    if (req.query.workerId) {
      query.workerId = req.query.workerId;
    } else if (req.user.role === 'WORKER') {
      query.workerId = req.user.workerId;
    }

    // Filter by Risk Level
    if (req.query.riskLevel) {
      query.riskLevel = req.query.riskLevel.toUpperCase();
    }

    // Search query by worker name or band ID
    if (req.query.search) {
      query.$or = [
        { workerName: { $regex: req.query.search, $options: 'i' } },
        { bandId: { $regex: req.query.search, $options: 'i' } }
      ];
    }

    const totalLogs = await ShiftLog.countDocuments(query);
    const logs = await ShiftLog.find(query)
      .sort({ createdAt: -1 })
      .skip(skip)
      .limit(limit);

    return res.status(200).json({
      success: true,
      count: logs.length,
      total: totalLogs,
      page,
      pages: Math.ceil(totalLogs / limit),
      data: logs
    });
  } catch (error) {
    return res.status(500).json({ success: false, message: error.message });
  }
};

/**
 * @route   GET /api/dosimeter/dashboard
 * @desc    Get enterprise plant-wide risk analytics summary
 * @access  Private
 */
exports.getDashboardMetrics = async (req, res) => {
  try {
    const totalScans = await ShiftLog.countDocuments({});
    const safeCount = await ShiftLog.countDocuments({ riskLevel: 'SAFE' });
    const cautionCount = await ShiftLog.countDocuments({ riskLevel: 'CAUTION' });
    const highCount = await ShiftLog.countDocuments({ riskLevel: 'HIGH' });
    const criticalCount = await ShiftLog.countDocuments({ riskLevel: 'CRITICAL' });

    const totalDoseAgg = await ShiftLog.aggregate([
      { $group: { _id: null, totalDose: { $sum: '$estimatedDosePpmHr' }, avgTwa: { $avg: '$twa8hrPpm' } } }
    ]);

    const totalDose = totalDoseAgg.length > 0 ? totalDoseAgg[0].totalDose : 0.0;
    const avgTwa = totalDoseAgg.length > 0 ? totalDoseAgg[0].avgTwa : 0.0;

    return res.status(200).json({
      success: true,
      metrics: {
        totalScans,
        safeCount,
        cautionCount,
        highCount,
        criticalCount,
        totalDosePpmHr: roundNum(totalDose),
        avgTwaPpm: roundNum(avgTwa),
        complianceRatePct: totalScans > 0 ? roundNum(((safeCount + cautionCount) / totalScans) * 100) : 100.0
      }
    });
  } catch (error) {
    return res.status(500).json({ success: false, message: error.message });
  }
};

function roundNum(val) {
  return Math.round(val * 100) / 100;
}
