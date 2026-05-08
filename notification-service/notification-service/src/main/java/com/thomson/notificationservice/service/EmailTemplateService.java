package com.thomson.notificationservice.service;

import org.springframework.stereotype.Service;

@Service
public class EmailTemplateService {

    public String bookingConfirmed(Long bookingId, String seatNumber, Double totalPrice) {
        return base(
            "#1a7f4b",
            "&#10003; Booking Confirmed",
            "Your flight booking has been confirmed. We look forward to flying with you!",
            row("Booking ID", "#" + bookingId) +
            row("Seat Number", seatNumber) +
            row("Amount Paid", "&#8358;" + String.format("%,.2f", totalPrice)) +
            row("Status", "<span style='color:#1a7f4b;font-weight:600'>CONFIRMED</span>"),
            "Please arrive at the airport at least 2 hours before departure. Keep this email as your booking reference."
        );
    }

    public String bookingCancelled(Long bookingId, String seatNumber) {
        return base(
            "#c0392b",
            "&#10007; Booking Cancelled",
            "Your booking has been cancelled and your seat has been released.",
            row("Booking ID", "#" + bookingId) +
            row("Seat Number", seatNumber) +
            row("Status", "<span style='color:#c0392b;font-weight:600'>CANCELLED</span>"),
            "If you did not request this cancellation or need assistance, please contact our support team."
        );
    }

    public String paymentSuccess(Long bookingId, Double amount, Long paymentReference) {
        return base(
            "#1a7f4b",
            "&#10003; Payment Successful",
            "Your payment has been received and processed successfully.",
            row("Payment Reference", String.valueOf(paymentReference)) +
            row("Booking ID", "#" + bookingId) +
            row("Amount Charged", "&#8358;" + String.format("%,.2f", amount)) +
            row("Status", "<span style='color:#1a7f4b;font-weight:600'>SUCCESS</span>"),
            "This is your official payment receipt. Please save this email for your records."
        );
    }

    public String paymentFailed(Long bookingId, Double amount) {
        return base(
            "#c0392b",
            "&#10007; Payment Failed",
            "Unfortunately your payment could not be processed.",
            row("Booking ID", "#" + bookingId) +
            row("Amount", "&#8358;" + String.format("%,.2f", amount)) +
            row("Status", "<span style='color:#c0392b;font-weight:600'>FAILED</span>"),
            "Please try again or use a different payment method. Your seat reservation may expire if payment is not completed."
        );
    }

    private String row(String label, String value) {
        return "<tr>" +
               "<td style='padding:10px 16px;color:#6b7280;font-size:14px;border-bottom:1px solid #f3f4f6;width:40%'>" + label + "</td>" +
               "<td style='padding:10px 16px;color:#111827;font-size:14px;font-weight:500;border-bottom:1px solid #f3f4f6'>" + value + "</td>" +
               "</tr>";
    }

    private String base(String accentColor, String heading, String subheading, String rows, String footerNote) {
        return "<!DOCTYPE html><html><head><meta charset='UTF-8'></head><body style='margin:0;padding:0;background:#f3f4f6;font-family:Arial,sans-serif'>" +
               "<table width='100%' cellpadding='0' cellspacing='0' style='background:#f3f4f6;padding:40px 0'><tr><td align='center'>" +
               "<table width='600' cellpadding='0' cellspacing='0' style='background:#ffffff;border-radius:8px;overflow:hidden;box-shadow:0 2px 8px rgba(0,0,0,0.08)'>" +

               // Header
               "<tr><td style='background:#0f172a;padding:28px 32px'>" +
               "<table width='100%'><tr>" +
               "<td style='color:#ffffff;font-size:22px;font-weight:700;letter-spacing:1px'>&#9992; AirSync</td>" +
               "<td align='right' style='color:#94a3b8;font-size:12px'>Airline Booking Platform</td>" +
               "</tr></table>" +
               "</td></tr>" +

               // Status banner
               "<tr><td style='background:" + accentColor + ";padding:20px 32px'>" +
               "<p style='margin:0;color:#ffffff;font-size:20px;font-weight:600'>" + heading + "</p>" +
               "<p style='margin:6px 0 0;color:rgba(255,255,255,0.85);font-size:14px'>" + subheading + "</p>" +
               "</td></tr>" +

               // Details table
               "<tr><td style='padding:24px 32px'>" +
               "<p style='margin:0 0 16px;color:#374151;font-size:15px;font-weight:600'>Booking Details</p>" +
               "<table width='100%' cellpadding='0' cellspacing='0' style='border:1px solid #e5e7eb;border-radius:6px;border-collapse:collapse'>" +
               rows +
               "</table>" +
               "</td></tr>" +

               // Note
               "<tr><td style='padding:0 32px 24px'>" +
               "<table width='100%' cellpadding='0' cellspacing='0' style='background:#f8fafc;border-left:4px solid " + accentColor + ";border-radius:4px;padding:14px 16px'>" +
               "<tr><td style='color:#475569;font-size:13px;line-height:1.6'>" + footerNote + "</td></tr>" +
               "</table>" +
               "</td></tr>" +

               // Footer
               "<tr><td style='background:#f8fafc;padding:20px 32px;border-top:1px solid #e5e7eb'>" +
               "<table width='100%'><tr>" +
               "<td style='color:#9ca3af;font-size:12px'>&#169; 2026 AirSync. All rights reserved.</td>" +
               "<td align='right' style='color:#9ca3af;font-size:12px'>This is an automated message — please do not reply.</td>" +
               "</tr></table>" +
               "</td></tr>" +

               "</table>" +
               "</td></tr></table>" +
               "</body></html>";
    }
}
