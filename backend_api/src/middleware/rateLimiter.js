const rateLimit = require('express-rate-limit');

/**
 * Global rate limiter to prevent API abuse and brute-force attacks.
 * Limit: 100 requests per 15 minutes window per IP.
 */
const apiLimiter = rateLimit({
  windowMs: 15 * 60 * 1000, // 15 minutes
  max: 100,
  message: {
    success: false,
    message: 'Too many requests from this IP, please try again after 15 minutes'
  },
  standardHeaders: true,
  legacyHeaders: false
});

/**
 * Strict rate limiter for Auth login/register routes.
 * Limit: 10 attempts per 15 minutes window per IP.
 */
const authLimiter = rateLimit({
  windowMs: 15 * 60 * 1000,
  max: 10,
  message: {
    success: false,
    message: 'Too many authentication attempts. Please try again after 15 minutes'
  }
});

module.exports = { apiLimiter, authLimiter };
