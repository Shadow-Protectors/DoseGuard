const express = require('express');
const router = express.Router();
const { generateFormIVPDF } = require('../controllers/reportController');
const { protect } = require('../middleware/auth');

router.get('/dgms-form-iv/:logId', protect, generateFormIVPDF);

module.exports = router;
