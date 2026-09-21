const mongoose = require('mongoose');

const shiftLogSchema = new mongoose.Schema({
  logId: {
    type: String,
    required: true,
    unique: true,
    index: true
  },
  workerId: {
    type: String,
    required: true,
    index: true
  },
  workerName: {
    type: String,
    required: true
  },
  department: {
    type: String,
    default: 'Refinery Sweetening Unit'
  },
  bandId: {
    type: String,
    required: true,
    index: true
  },
  shiftDate: {
    type: String,
    required: true,
    index: true
  },
  startTime: {
    type: String,
    required: true
  },
  scanTime: {
    type: String,
    required: true
  },
  durationHours: {
    type: Number,
    default: 8.0
  },
  expiryStatus: {
    type: String,
    enum: ['VALID', 'EXPIRED'],
    default: 'VALID'
  },
  rawDeltaE: {
    type: Number,
    required: true
  },
  estimatedDosePpmHr: {
    type: Number,
    required: true
  },
  uncertaintyPpmHr: {
    type: Number,
    required: true
  },
  twa8hrPpm: {
    type: Number,
    required: true
  },
  riskLevel: {
    type: String,
    enum: ['SAFE', 'CAUTION', 'HIGH', 'CRITICAL'],
    default: 'SAFE',
    index: true
  },
  actionRequired: {
    type: String,
    required: true
  },
  imageHash: {
    type: String,
    required: true
  },
  location: {
    type: {
      type: String,
      enum: ['Point'],
      default: 'Point'
    },
    coordinates: {
      type: [Number], // [longitude, latitude]
      default: [77.5946, 12.9716]
    }
  },
  syncStatus: {
    type: String,
    enum: ['PENDING', 'SYNCED'],
    default: 'SYNCED'
  }
}, {
  timestamps: true
});

// 2DSphere spatial index for geo-location heatmaps
shiftLogSchema.index({ location: '2dsphere' });

module.exports = mongoose.model('ShiftLog', shiftLogSchema);
