const express = require('express');
const router = express.Router();
const { syncShiftLogs, getShiftLogs, getDashboardMetrics } = require('../controllers/dosimeterController');
const { protect } = require('../middleware/auth');

router.post('/sync', protect, syncShiftLogs);
router.get('/logs', protect, getShiftLogs);
router.get('/dashboard', protect, getDashboardMetrics);

module.exports = router;
