const express = require('express');
const cors = require('cors');
const helmet = require('helmet');
const { apiLimiter } = require('./middleware/rateLimiter');

const authRoutes = require('./routes/authRoutes');
const dosimeterRoutes = require('./routes/dosimeterRoutes');
const reportRoutes = require('./routes/reportRoutes');

const app = express();

// Security Middlewares
app.use(helmet());
app.use(cors());
app.use(express.json({ limit: '10mb' }));
app.use(express.urlencoded({ extended: true, limit: '10mb' }));

// Apply rate limiting
app.use('/api', apiLimiter);

// Health check endpoint
app.get('/api/health', (req, res) => {
  res.status(200).json({
    status: 'UP',
    service: 'SIH 26118 Dosimeter API Engine',
    timestamp: new Date().toISOString()
  });
});

// API Routes
app.use('/api/auth', authRoutes);
app.use('/api/dosimeter', dosimeterRoutes);
app.use('/api/reports', reportRoutes);

// Global 404 handler
app.use((req, res) => {
  res.status(404).json({ success: false, message: 'Resource endpoint not found' });
});

// Global error handler
app.use((err, req, res, next) => {
  console.error('[API Error]', err);
  res.status(err.status || 500).json({
    success: false,
    message: err.message || 'Internal Server Error'
  });
});

module.exports = app;
