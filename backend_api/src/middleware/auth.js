const jwt = require('jsonwebtoken');
const User = require('../models/User');

/**
 * Protects routes requiring valid JWT Bearer authentication.
 */
const protect = async (req, res, next) => {
  let token;

  if (req.headers.authorization && req.headers.authorization.startsWith('Bearer')) {
    try {
      token = req.headers.authorization.split(' ')[1];
      const decoded = jwt.verify(token, process.env.JWT_SECRET || 'sih_26118_ultra_secure_jwt_secret_key_2026');
      
      req.user = await User.findById(decoded.id).select('-password');
      if (!req.user) {
        return res.status(401).json({ success: false, message: 'User account no longer exists' });
      }
      return next();
    } catch (error) {
      return res.status(401).json({ success: false, message: 'Not authorized, token validation failed' });
    }
  }

  if (!token) {
    return res.status(401).json({ success: false, message: 'Not authorized, missing bearer token' });
  }
};

/**
 * Enforces Role-Based Access Control (RBAC).
 */
const authorize = (...roles) => {
  return (req, res, next) => {
    if (!req.user || !roles.includes(req.user.role)) {
      return res.status(403).json({
        success: false,
        message: `User role '${req.user ? req.user.role : 'UNKNOWN'}' is not authorized to access this route`
      });
    }
    next();
  };
};

module.exports = { protect, authorize };
