require('dotenv').config();
const app = require('./src/app');
const connectDB = require('./src/config/db');

const PORT = process.env.PORT || 5000;

// Connect to MongoDB Database
connectDB();

const server = app.listen(PORT, () => {
  console.log(`=======================================================`);
  console.log(`[SIH 26118 API] Server running on http://localhost:${PORT}`);
  console.log(`[Environment] ${process.env.NODE_ENV || 'development'}`);
  console.log(`=======================================================`);
});

// Handle unhandled promise rejections
process.on('unhandledRejection', (err) => {
  console.error(`[Unhandled Rejection] ${err.message}`);
  server.close(() => process.exit(1));
});
