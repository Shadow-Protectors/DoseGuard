"""
DoseGuard — Industrial QR Code and Barcode Generator
Generates scannable QR codes and 1D Code-128 barcodes for workers and dosimeter bands,
plus an interactive HTML printable test sheet.
"""

import os
import qrcode
from PIL import Image, ImageDraw, ImageFont
import barcode
from barcode.writer import ImageWriter

OUTPUT_DIR = "test_qrs_and_barcodes"
os.makedirs(OUTPUT_DIR, exist_ok=True)

# ── 1. Worker Test Dataset ──────────────────────────────────────────────────
WORKERS = [
    {
        "emp_id": "EMP-1052",
        "name": "Arun Kumar",
        "department": "Gas Processing",
        "role": "Process Operator",
        "shift": "Morning (06:00 - 14:00)",
        "qr_payload": "EMP-1052",
        "alt_qr_payload": "DG:WORKER:EMP-1052",
        "notes": "Valid pre-enrolled worker. Test manual entry or barcode/QR scan."
    },
    {
        "emp_id": "EMP02345",
        "name": "John Mathew",
        "department": "Gas Processing",
        "role": "Field Technician",
        "shift": "Morning (06:00 - 14:00)",
        "qr_payload": "EMP02345",
        "alt_qr_payload": "DG:WORKER:EMP02345",
        "notes": "Target worker from specification prompt. Fresh band ready for assignment."
    },
    {
        "emp_id": "EMP02346",
        "name": "Anita Desai",
        "department": "Desulfurization Unit",
        "role": "Safety Inspector",
        "shift": "Evening (14:00 - 22:00)",
        "qr_payload": "EMP02346",
        "alt_qr_payload": "DG:WORKER:EMP02346",
        "notes": "Currently wears active band WB-ASSIGNED-01."
    },
    {
        "emp_id": "EMP-7821",
        "name": "Rajesh Kumar",
        "department": "Refinery Sweetening Unit",
        "role": "Plant Operator",
        "shift": "Morning (06:00 - 14:00)",
        "qr_payload": "EMP-7821",
        "alt_qr_payload": "DG:WORKER:EMP-7821",
        "notes": "Assigned band WB-1001."
    },
    {
        "emp_id": "EMP-9043",
        "name": "Priya Sharma",
        "department": "Catalytic Cracking Unit",
        "role": "Shift Supervisor",
        "shift": "Night (22:00 - 06:00)",
        "qr_payload": "EMP-9043",
        "alt_qr_payload": "DG:WORKER:EMP-9043",
        "notes": "Valid registered supervisor."
    },
    {
        "emp_id": "EMP99999",
        "name": "Unknown Visitor",
        "department": "Contractor / Unregistered",
        "role": "Guest",
        "shift": "None",
        "qr_payload": "EMP99999",
        "alt_qr_payload": "DG:WORKER:EMP99999",
        "notes": "Negative Test: Triggers 'Worker not found in database' error state."
    }
]

# ── 2. Dosimeter Band Test Dataset ──────────────────────────────────────────
BANDS = [
    {
        "band_id": "BAND-001285",
        "batch_no": "BATCH-2026-A1",
        "status": "AVAILABLE",
        "expected_result": "VALID (Proceeds to Confirmation Screen)",
        "qr_payload": "DG:BAND:BAND-001285",
        "bare_payload": "BAND-001285",
        "badge_color": "#10B981",
        "notes": "Primary valid band from spec prompt. Ready for immediate assignment."
    },
    {
        "band_id": "WB-2005",
        "batch_no": "BATCH-2026-A2",
        "status": "AVAILABLE",
        "expected_result": "VALID (Proceeds to Confirmation Screen)",
        "qr_payload": "DG:BAND:WB-2005",
        "bare_payload": "WB-2005",
        "badge_color": "#10B981",
        "notes": "Fresh inventory dosimeter band. 30 days chemical shelf-life."
    },
    {
        "band_id": "WB-ASSIGNED-01",
        "batch_no": "BATCH-2026-A1",
        "status": "ACTIVE / ASSIGNED",
        "expected_result": "ERROR: 'Already assigned to Anita Desai (EMP02346)'",
        "qr_payload": "DG:BAND:WB-ASSIGNED-01",
        "bare_payload": "WB-ASSIGNED-01",
        "badge_color": "#F59E0B",
        "notes": "Negative Test: Rejection gate prevents duplicate active wearers."
    },
    {
        "band_id": "WB-EXP-01",
        "batch_no": "BATCH-2025-Z9",
        "status": "EXPIRED",
        "expected_result": "ERROR: 'Chemical shelf life expired. Sensor no longer calibrated.'",
        "qr_payload": "DG:BAND:WB-EXP-01",
        "bare_payload": "WB-EXP-01",
        "badge_color": "#EF4444",
        "notes": "Negative Test: Lead acetate reagent past 30-day calibration window."
    },
    {
        "band_id": "WB-SAT-99",
        "batch_no": "BATCH-2026-X1",
        "status": "SATURATED",
        "expected_result": "ERROR: 'Strip saturated (50.0 / 50.0 ppm·hr). Replace the band.'",
        "qr_payload": "DG:BAND:WB-SAT-99",
        "bare_payload": "WB-SAT-99",
        "badge_color": "#EF4444",
        "notes": "Negative Test: Optical density maxed out, chemical reagent fully converted."
    },
    {
        "band_id": "BAND-UNKNOWN-999",
        "batch_no": "UNKNOWN",
        "status": "UNREGISTERED",
        "expected_result": "ERROR: 'Unknown band. Not registered in the system.'",
        "qr_payload": "DG:BAND:BAND-UNKNOWN-999",
        "bare_payload": "BAND-UNKNOWN-999",
        "badge_color": "#64748B",
        "notes": "Negative Test: Band ID not present in plant Room database."
    }
]

def make_qr_image(payload: str, filename: str, box_size: int = 10, border: int = 2) -> str:
    qr = qrcode.QRCode(
        version=None,
        error_correction=qrcode.constants.ERROR_CORRECT_M,
        box_size=box_size,
        border=border
    )
    qr.add_data(payload)
    qr.make(fit=True)
    img = qr.make_image(fill_color="black", back_color="white")
    filepath = os.path.join(OUTPUT_DIR, filename)
    img.save(filepath)
    return filepath

def make_barcode_1d(payload: str, filename: str) -> str:
    code128 = barcode.get_barcode_class('code128')
    writer = ImageWriter()
    writer.set_options({
        'module_width': 0.3,
        'module_height': 15.0,
        'font_size': 10,
        'text_distance': 4.0,
        'quiet_zone': 2.5
    })
    bc = code128(payload, writer=writer)
    base_name = os.path.join(OUTPUT_DIR, filename.replace('.png', ''))
    saved_path = bc.save(base_name)
    return saved_path

print("[1/3] Generating Worker QR Codes and 1D Barcodes...")
worker_files = []
for w in WORKERS:
    safe_id = w["emp_id"].replace("-", "_")
    qr_fn = f"worker_qr_{safe_id}.png"
    bc_fn = f"worker_barcode_{safe_id}.png"
    qr_path = make_qr_image(w["qr_payload"], qr_fn, box_size=8)
    bc_path = make_barcode_1d(w["emp_id"], bc_fn)
    worker_files.append({**w, "qr_path": qr_fn, "bc_path": os.path.basename(bc_path)})
    print(f"  [OK] {w['emp_id']} ({w['name']}) -> QR: {qr_fn}, 1D: {os.path.basename(bc_path)}")

print("\n[2/3] Generating Dosimeter Band QR Codes...")
band_files = []
for b in BANDS:
    safe_id = b["band_id"].replace("-", "_")
    qr_fn = f"band_qr_{safe_id}.png"
    qr_path = make_qr_image(b["qr_payload"], qr_fn, box_size=8)
    band_files.append({**b, "qr_path": qr_fn})
    print(f"  [OK] {b['band_id']} [{b['status']}] -> QR: {qr_fn}")

import base64

def get_base64_img(filename: str) -> str:
    path = os.path.join(OUTPUT_DIR, filename)
    with open(path, "rb") as img_file:
        b64 = base64.b64encode(img_file.read()).decode("utf-8")
        return f"data:image/png;base64,{b64}"

print("\n[3/3] Generating Interactive HTML Printable Test Sheet with embedded base64 assets...")

html_content = f"""<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<title>DoseGuard Industrial Test Codes — Workers & Dosimeter Bands</title>
<style>
  :root {{
    --primary: #0D47A1;
    --primary-light: #E3F2FD;
    --accent: #0284C7;
    --surface: #F8FAFC;
    --card: #FFFFFF;
    --border: #E2E8F0;
    --text-primary: #0F172A;
    --text-secondary: #475569;
    --success: #10B981;
    --warning: #F59E0B;
    --danger: #EF4444;
  }}
  * {{ box-sizing: border-box; margin: 0; padding: 0; }}
  body {{
    font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif;
    background-color: var(--surface);
    color: var(--text-primary);
    padding: 24px;
    line-height: 1.5;
  }}
  .header {{
    background: linear-gradient(135deg, #0D47A1 0%, #1565C0 60%, #0284C7 100%);
    color: white;
    padding: 32px 28px;
    border-radius: 16px;
    margin-bottom: 28px;
    box-shadow: 0 10px 25px -5px rgba(13, 71, 161, 0.25);
  }}
  .header h1 {{ font-size: 28px; font-weight: 800; margin-bottom: 8px; letter-spacing: -0.5px; }}
  .header p {{ font-size: 15px; opacity: 0.92; max-width: 800px; }}
  .header .badge {{
    display: inline-block;
    background: rgba(255, 255, 255, 0.2);
    border: 1px solid rgba(255, 255, 255, 0.35);
    padding: 4px 12px;
    border-radius: 999px;
    font-size: 12px;
    font-weight: 700;
    margin-top: 14px;
    text-transform: uppercase;
    letter-spacing: 0.5px;
  }}
  .section-title {{
    font-size: 20px;
    font-weight: 800;
    color: var(--primary);
    margin: 32px 0 16px 0;
    display: flex;
    align-items: center;
    gap: 10px;
  }}
  .section-title span.tag {{
    font-size: 12px;
    font-weight: 600;
    background: var(--primary-light);
    color: var(--primary);
    padding: 2px 10px;
    border-radius: 6px;
  }}
  .grid {{
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(360px, 1fr));
    gap: 20px;
  }}
  .card {{
    background: var(--card);
    border: 1px solid var(--border);
    border-radius: 14px;
    padding: 20px;
    box-shadow: 0 4px 12px rgba(0,0,0,0.03);
    transition: transform 0.15s ease, box-shadow 0.15s ease;
    display: flex;
    flex-direction: column;
  }}
  .card:hover {{
    transform: translateY(-2px);
    box-shadow: 0 8px 20px rgba(0,0,0,0.06);
  }}
  .card-top {{
    display: flex;
    justify-content: space-between;
    align-items: flex-start;
    margin-bottom: 12px;
  }}
  .worker-name {{ font-size: 17px; font-weight: 700; color: var(--text-primary); }}
  .emp-id-pill {{
    font-family: monospace;
    font-size: 13px;
    font-weight: 700;
    background: #F1F5F9;
    color: #334155;
    padding: 3px 8px;
    border-radius: 6px;
    border: 1px solid #CBD5E1;
  }}
  .meta-row {{
    font-size: 13px;
    color: var(--text-secondary);
    margin-bottom: 4px;
  }}
  .meta-row strong {{ color: var(--text-primary); }}
  .code-container {{
    background: #FAFAFA;
    border: 1px dashed var(--border);
    border-radius: 10px;
    padding: 14px;
    margin: 14px 0;
    text-align: center;
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 12px;
  }}
  .code-container img {{
    max-width: 100%;
    height: auto;
    background: white;
    padding: 6px;
    border-radius: 6px;
    box-shadow: 0 2px 6px rgba(0,0,0,0.05);
  }}
  .code-container .qr-img {{
    width: 180px;
    height: 180px;
  }}
  .code-container .barcode-img {{
    max-height: 85px;
    width: auto;
  }}
  .status-pill {{
    display: inline-block;
    font-size: 11px;
    font-weight: 700;
    padding: 3px 10px;
    border-radius: 999px;
    text-transform: uppercase;
    letter-spacing: 0.5px;
  }}
  .status-available {{ background: #DCFCE7; color: #166534; }}
  .status-active {{ background: #FEF3C7; color: #92400E; }}
  .status-expired {{ background: #FEE2E2; color: #991B1B; }}
  .status-saturated {{ background: #FEE2E2; color: #991B1B; }}
  .status-unregistered {{ background: #F1F5F9; color: #475569; }}

  .notes-box {{
    background: #F8FAFC;
    border-left: 3px solid var(--accent);
    padding: 8px 12px;
    font-size: 12px;
    color: var(--text-secondary);
    border-radius: 0 6px 6px 0;
    margin-top: auto;
  }}
  .payload-raw {{
    font-family: monospace;
    font-size: 11px;
    color: #0369A1;
    word-break: break-all;
    background: #E0F2FE;
    padding: 4px 8px;
    border-radius: 4px;
    margin-top: 6px;
    display: block;
  }}
  @media print {{
    body {{ background: white; padding: 0; }}
    .header {{ box-shadow: none; }}
    .card {{ break-inside: avoid; border: 1px solid #ccc; }}
  }}
</style>
</head>
<body>

<div class="header">
  <h1>DoseGuard Industrial H₂S Dosimeter — Test Barcodes & QR Codes</h1>
  <p>High-contrast, scannable test codes for testing the <strong>Worker-First Dosimeter Band Assignment</strong> workflow on Android devices or emulators with camera/webcam pass-through.</p>
  <span class="badge">SIH 2026 Ready · Material 3 Verified</span>
</div>

<!-- SECTION 1: WORKER IDENTIFICATION -->
<div class="section-title">
  <span>1. Worker ID Cards & Badges</span>
  <span class="tag">Screen 1 — Worker Identification</span>
</div>
<div class="grid">
"""

for w in worker_files:
    is_neg = "99999" in w["emp_id"]
    status_class = "status-unregistered" if is_neg else "status-available"
    status_text = "Unregistered Test" if is_neg else "Pre-Enrolled"
    html_content += f"""
  <div class="card">
    <div class="card-top">
      <div>
        <div class="worker-name">{w['name']}</div>
        <div class="meta-row"><strong>Dept:</strong> {w['department']}</div>
        <div class="meta-row"><strong>Role:</strong> {w['role']}</div>
        <div class="meta-row"><strong>Shift:</strong> {w['shift']}</div>
      </div>
      <div>
        <span class="emp-id-pill">{w['emp_id']}</span>
      </div>
    </div>

    <div class="code-container">
      <div style="font-size: 12px; font-weight: 600; color: #475569;">2D QR Code</div>
      <img src="{get_base64_img(w['qr_path'])}" class="qr-img" alt="QR {w['emp_id']}" />
      <span class="payload-raw">{w['qr_payload']}</span>

      <div style="font-size: 12px; font-weight: 600; color: #475569; margin-top: 8px;">1D Code-128 Barcode</div>
      <img src="{get_base64_img(w['bc_path'])}" class="barcode-img" alt="Barcode {w['emp_id']}" />
    </div>

    <div class="notes-box">
      <strong>Test Note:</strong> {w['notes']}
    </div>
  </div>
"""

html_content += """
</div>

<!-- SECTION 2: DOSIMETER BANDS -->
<div class="section-title">
  <span>2. Dosimeter Wristband QR Tags</span>
  <span class="tag">Screen 2 — Scan Band QR & Validation Gate</span>
</div>
<div class="grid">
"""

for b in band_files:
    st = b["status"]
    if "AVAILABLE" in st:
        st_class = "status-available"
    elif "ACTIVE" in st or "ASSIGNED" in st:
        st_class = "status-active"
    elif "EXPIRED" in st:
        st_class = "status-expired"
    elif "SATURATED" in st:
        st_class = "status-saturated"
    else:
        st_class = "status-unregistered"

    html_content += f"""
  <div class="card">
    <div class="card-top">
      <div>
        <div class="worker-name" style="font-family: monospace; font-size: 18px;">{b['band_id']}</div>
        <div class="meta-row"><strong>Batch:</strong> {b['batch_no']}</div>
      </div>
      <div>
        <span class="status-pill {st_class}">{b['status']}</span>
      </div>
    </div>

    <div class="code-container">
      <div style="font-size: 12px; font-weight: 600; color: #475569;">Wristband QR Code</div>
      <img src="{get_base64_img(b['qr_path'])}" class="qr-img" alt="Band QR {b['band_id']}" />
      <span class="payload-raw">{b['qr_payload']}</span>
    </div>

    <div style="margin-bottom: 8px; font-size: 12px;">
      <strong>Expected Validation Result:</strong><br/>
      <span style="color: {b['badge_color']}; font-weight: 700;">{b['expected_result']}</span>
    </div>

    <div class="notes-box">
      <strong>Workflow Test:</strong> {b['notes']}
    </div>
  </div>
"""

html_content += """
</div>

<footer style="margin-top: 40px; padding: 20px; text-align: center; color: #64748B; font-size: 13px; border-top: 1px solid #E2E8F0;">
  DoseGuard Industrial H₂S Dosimeter Management System · Smart India Hackathon (SIH 2026)
</footer>

</body>
</html>
"""

html_file = "TEST_BARCODES_AND_QRS.html"
with open(html_file, "w", encoding="utf-8") as f:
    f.write(html_content)

print(f"\n[DONE] Successfully created:\n  - Images folder: {OUTPUT_DIR}/\n  - Interactive printable sheet: {html_file}")
