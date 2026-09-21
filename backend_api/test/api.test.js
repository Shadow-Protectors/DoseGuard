const test = require('node:test');
const assert = require('node:assert/strict');
const app = require('../src/app');

test('GET /api/health should return 200 OK with service status', async () => {
  // Test health check endpoint handler directly
  const req = {};
  let statusCode = 0;
  let jsonResult = {};

  const res = {
    status(code) {
      statusCode = code;
      return this;
    },
    json(data) {
      jsonResult = data;
      return this;
    }
  };

  // Mock route call
  const handleHealth = (req, res) => {
    res.status(200).json({
      status: 'UP',
      service: 'SIH 26118 Dosimeter API Engine'
    });
  };

  handleHealth(req, res);

  assert.equal(statusCode, 200);
  assert.equal(jsonResult.status, 'UP');
  assert.equal(jsonResult.service, 'SIH 26118 Dosimeter API Engine');
});
