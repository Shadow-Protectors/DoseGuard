"""
SIH 26118 - Statutory DGMS Form IV & OISD 155 PDF Report Exporter

Generates official PDF compliance reports formatted per Directorate General of Mines Safety (DGMS)
Form IV occupational health registers and Oil Industry Safety Directorate (OISD) Standard 155 guidelines.
"""

from io import BytesIO
from typing import Dict, Any, List
from reportlab.lib.pagesizes import letter
from reportlab.lib import colors
from reportlab.platypus import SimpleDocTemplate, Paragraph, Spacer, Table, TableStyle, HRFlowable
from reportlab.lib.styles import getSampleStyleSheet, ParagraphStyle


def generate_dgms_form_iv_pdf(
    log_data: Dict[str, Any],
    worker_data: Dict[str, Any]
) -> bytes:
    """
    Generates a printable DGMS Form IV PDF document in memory.

    Parameters:
    -----------
    log_data : Dict[str, Any]
        Shift log record dictionary.
    worker_data : Dict[str, Any]
        Worker profile dictionary.

    Returns:
    --------
    bytes
        Raw PDF document bytes.

    Complexity:
    -----------
    Time Complexity: O(1)
    Space Complexity: O(M) where M is generated PDF buffer size.
    """
    buffer = BytesIO()
    doc = SimpleDocTemplate(
        buffer,
        pagesize=letter,
        rightMargin=36,
        leftMargin=36,
        topMargin=36,
        bottomMargin=36
    )

    styles = getSampleStyleSheet()
    
    # Custom Styles
    title_style = ParagraphStyle(
        'DocTitle',
        parent=styles['Heading1'],
        fontSize=14,
        leading=16,
        alignment=1,  # Centered
        textColor=colors.HexColor("#1A2B4C")
    )
    
    subtitle_style = ParagraphStyle(
        'DocSubTitle',
        parent=styles['Normal'],
        fontSize=9,
        leading=11,
        alignment=1,
        textColor=colors.HexColor("#4A5568")
    )

    header_style = ParagraphStyle(
        'SectionHeader',
        parent=styles['Heading2'],
        fontSize=11,
        leading=13,
        textColor=colors.HexColor("#2B6CB0"),
        spaceBefore=10,
        spaceAfter=6
    )

    cell_bold = ParagraphStyle('CellBold', parent=styles['Normal'], fontSize=9, leading=11, fontName="Helvetica-Bold")
    cell_norm = ParagraphStyle('CellNorm', parent=styles['Normal'], fontSize=9, leading=11)

    story = []

    # Title & Subtitle Header
    story.append(Paragraph("DIRECTORATE GENERAL OF MINES SAFETY (DGMS)", title_style))
    story.append(Paragraph("FORM IV - STATUTORY REGISTER OF OCCUPATIONAL HEALTH SURVEILLANCE", title_style))
    story.append(Spacer(1, 4))
    story.append(Paragraph("Pursuant to Rule 29O of Mines Rules / OISD Standard 155 Toxic Gas Protocol", subtitle_style))
    story.append(Spacer(1, 10))
    story.append(HRFlowable(width="100%", thickness=1.5, color=colors.HexColor("#2B6CB0"), spaceAfter=10))

    # Section 1: Worker & Facility Profile Table
    story.append(Paragraph("1. Worker Profile & Shift Identifiers", header_style))
    
    profile_table_data = [
        [Paragraph("Worker ID:", cell_bold), Paragraph(str(worker_data.get("worker_id", "N/A")), cell_norm),
         Paragraph("Full Name:", cell_bold), Paragraph(str(worker_data.get("name", "N/A")), cell_norm)],
        [Paragraph("Department:", cell_bold), Paragraph(str(worker_data.get("department", "N/A")), cell_norm),
         Paragraph("Designation/Role:", cell_bold), Paragraph(str(worker_data.get("role", "N/A")), cell_norm)],
        [Paragraph("Shift Date:", cell_bold), Paragraph(str(log_data.get("shift_date", "N/A")), cell_norm),
         Paragraph("Scan Time:", cell_bold), Paragraph(str(log_data.get("scan_time", "N/A")), cell_norm)],
        [Paragraph("Wristband ID:", cell_bold), Paragraph(str(log_data.get("band_id", "N/A")), cell_norm),
         Paragraph("Shift Duration:", cell_bold), Paragraph(f"{log_data.get('duration_hours', 8.0)} Hours", cell_norm)]
    ]

    t1 = Table(profile_table_data, colWidths=[100, 160, 100, 160])
    t1.setStyle(TableStyle([
        ('BACKGROUND', (0, 0), (-1, -1), colors.HexColor("#F7FAFC")),
        ('GRID', (0, 0), (-1, -1), 0.5, colors.HexColor("#E2E8F0")),
        ('PADDING', (0, 0), (-1, -1), 5),
    ]))
    story.append(t1)
    story.append(Spacer(1, 12))

    # Section 2: Dosimetric Exposure Quantification Table
    story.append(Paragraph("2. Quantitative H₂S Exposure Findings", header_style))

    risk_level = str(log_data.get("risk_level", "SAFE")).upper()
    risk_color = colors.HexColor("#38A169") if risk_level == "SAFE" else (
        colors.HexColor("#DD6B20") if risk_level in ["CAUTION", "HIGH"] else colors.HexColor("#E53E3E")
    )

    dose_table_data = [
        [Paragraph("Parameter", cell_bold), Paragraph("Measured Value", cell_bold), Paragraph("Regulatory Threshold", cell_bold)],
        [Paragraph("Badge Expiry Status", cell_norm), Paragraph(str(log_data.get("expiry_status", "VALID")), cell_norm), Paragraph("Must be VALID", cell_norm)],
        [Paragraph("Optical Color Diff (ΔE)", cell_norm), Paragraph(f"{log_data.get('raw_delta_e', 0.0):.2f}", cell_norm), Paragraph("N/A (Optical Index)", cell_norm)],
        [Paragraph("Cumulative Dose (D)", cell_norm), Paragraph(f"{log_data.get('estimated_dose_ppm_hr', 0.0):.2f} ± {log_data.get('uncertainty_ppm_hr', 0.0):.2f} ppm·hr", cell_norm), Paragraph("80 ppm·hr (8-hr limit)", cell_norm)],
        [Paragraph("Normalized 8-Hr TWA", cell_norm), Paragraph(f"{log_data.get('twa_8hr_ppm', 0.0):.2f} ppm", cell_norm), Paragraph("10.0 ppm PEL / 2.5 ppm Action", cell_norm)],
        [Paragraph("Assessed Action Level", cell_bold), Paragraph(f"<font color='{risk_color.hexval()}'><b>{risk_level}</b></font>", cell_bold), Paragraph("OSHA / DGMS Standard", cell_norm)]
    ]

    t2 = Table(dose_table_data, colWidths=[180, 180, 160])
    t2.setStyle(TableStyle([
        ('BACKGROUND', (0, 0), (-1, 0), colors.HexColor("#EDF2F7")),
        ('GRID', (0, 0), (-1, -1), 0.5, colors.HexColor("#CBD5E0")),
        ('PADDING', (0, 0), (-1, -1), 6),
    ]))
    story.append(t2)
    story.append(Spacer(1, 12))

    # Section 3: Statutory Compliance & Audit Trail
    story.append(Paragraph("3. Cryptographic Audit Trail & Statutory Sign-Off", header_style))

    audit_table_data = [
        [Paragraph("Cryptographic Hash (SHA-256):", cell_bold), Paragraph(f"<font size=7>{log_data.get('image_hash', 'N/A')}</font>", cell_norm)],
        [Paragraph("Local DB Sync Status:", cell_bold), Paragraph(str(log_data.get("sync_status", "SYNCED")), cell_norm)],
        [Paragraph("Action Advice:", cell_bold), Paragraph(str(log_data.get("action_required", "None")), cell_norm)]
    ]

    t3 = Table(audit_table_data, colWidths=[160, 340])
    t3.setStyle(TableStyle([
        ('GRID', (0, 0), (-1, -1), 0.5, colors.HexColor("#E2E8F0")),
        ('PADDING', (0, 0), (-1, -1), 5),
    ]))
    story.append(t3)
    story.append(Spacer(1, 24))

    # Signature Block
    sig_data = [
        [Paragraph("_______________________________<br/><b>Worker Signature</b>", cell_norm),
         Paragraph("_______________________________<br/><b>Certified Safety Officer</b>", cell_norm)]
    ]
    t_sig = Table(sig_data, colWidths=[250, 250])
    t_sig.setStyle(TableStyle([('ALIGN', (0, 0), (-1, -1), 'CENTER')]))
    story.append(t_sig)

    doc.build(story)
    pdf_data = buffer.getvalue()
    buffer.close()
    return pdf_data
