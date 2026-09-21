const User = require('../models/User');
const jwt = require('jsonwebtoken');

// Helper to generate JWT token
const generateToken = (id) => {
  return jwt.sign({ id }, process.env.JWT_SECRET || 'sih_26118_ultra_secure_jwt_secret_key_2026', {
    expiresIn: process.env.JWT_EXPIRES_IN || '7d'
  });
};

/**
 * @route   POST /api/auth/register
 * @desc    Register a new user / worker
 * @access  Public
 */
exports.register = async (req, res) => {
  try {
    const { name, email, password, role, workerId, department, phone } = req.body;

    const existingUser = await User.findOne({ $or: [{ email }, { workerId }] });
    if (existingUser) {
      return res.status(400).json({
        success: false,
        message: 'User with this email or worker ID already exists'
      });
    }

    const user = await User.create({
      name,
      email,
      password,
      role: role || 'WORKER',
      workerId: workerId || `W-${Math.floor(1000 + Math.random() * 9000)}`,
      department: department || 'Refinery Sweetening Unit',
      phone: phone || '+91 98765 43210'
    });

    const token = generateToken(user._id);

    return res.status(201).json({
      success: true,
      message: 'Registration successful',
      token,
      user: {
        id: user._id,
        name: user.name,
        email: user.email,
        role: user.role,
        workerId: user.workerId,
        department: user.department,
        avatarUrl: user.avatarUrl
      }
    });
  } catch (error) {
    return res.status(500).json({ success: false, message: error.message });
  }
};

/**
 * @route   POST /api/auth/login
 * @desc    Authenticate user & return JWT token
 * @access  Public
 */
exports.login = async (req, res) => {
  try {
    const { email, password } = req.body;

    if (!email || !password) {
      return res.status(400).json({ success: false, message: 'Please provide email and password' });
    }

    const user = await User.findOne({ email }).select('+password');
    if (!user) {
      return res.status(401).json({ success: false, message: 'Invalid credentials' });
    }

    const isMatch = await user.matchPassword(password);
    if (!isMatch) {
      return res.status(401).json({ success: false, message: 'Invalid credentials' });
    }

    const token = generateToken(user._id);

    return res.status(200).json({
      success: true,
      message: 'Login successful',
      token,
      user: {
        id: user._id,
        name: user.name,
        email: user.email,
        role: user.role,
        workerId: user.workerId,
        department: user.department,
        avatarUrl: user.avatarUrl
      }
    });
  } catch (error) {
    return res.status(500).json({ success: false, message: error.message });
  }
};

/**
 * @route   POST /api/auth/forgot-password
 * @desc    Request password reset OTP
 * @access  Public
 */
exports.forgotPassword = async (req, res) => {
  try {
    const { email } = req.body;
    const user = await User.findOne({ email });
    if (!user) {
      return res.status(404).json({ success: false, message: 'No user registered with this email address' });
    }

    // Mock 6-digit OTP for industrial security demo
    const mockOtp = '849201';

    return res.status(200).json({
      success: true,
      message: 'Verification OTP sent to registered email',
      demoOtp: mockOtp
    });
  } catch (error) {
    return res.status(500).json({ success: false, message: error.message });
  }
};

/**
 * @route   POST /api/auth/verify-otp
 * @desc    Verify 6-digit OTP
 * @access  Public
 */
exports.verifyOtp = async (req, res) => {
  try {
    const { email, otp } = req.body;
    if (otp === '849201' || otp.length === 6) {
      return res.status(200).json({
        success: true,
        message: 'OTP verification successful. Proceed to set new password.'
      });
    }
    return res.status(400).json({ success: false, message: 'Invalid or expired OTP code' });
  } catch (error) {
    return res.status(500).json({ success: false, message: error.message });
  }
};

/**
 * @route   GET /api/auth/me
 * @desc    Get current user profile
 * @access  Private
 */
exports.getMe = async (req, res) => {
  try {
    const user = await User.findById(req.user.id);
    return res.status(200).json({ success: true, user });
  } catch (error) {
    return res.status(500).json({ success: false, message: error.message });
  }
};
