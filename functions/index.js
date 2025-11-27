const { setGlobalOptions } = require("firebase-functions/v2");
const { onRequest } = require("firebase-functions/v2/https");
const logger = require("firebase-functions/logger");
const admin = require("firebase-admin");
const express = require("express");
const cors = require("cors");
const nodemailer = require("nodemailer");

// Set global options
setGlobalOptions({ maxInstances: 10 });

// Initialize Firebase Admin
admin.initializeApp();
const db = admin.database();

// Setup Express app
const app = express();
app.use(cors({ origin: true }));
app.use(express.json());

// Setup Nodemailer transporter
const transporter = nodemailer.createTransport({
  service: "gmail",
  auth: {
    user: "ssenkubugeabbey055@gmail.com",
    pass: "ixzeukzvcamapsuo", // Gmail App Password
  },
});

// POST / => Send Appointment Email to Doctor
app.post("/", async (req, res) => {
  let { userName, doctorName, doctorEmail, specialization, date, time } = req.body;

  if (!userName || !doctorName || !doctorEmail || !specialization || !date || !time) {
    return res.status(400).json({ error: "Missing appointment details." });
  }

  // Normalize patient username key for Firebase paths
  const patientUsername = userName.trim().toLowerCase();

  // Compose confirmation link with username as 'user'
  const confirmationLink = `https://confirmappointment-vy6tk2hf5q-uc.a.run.app/confirm?user=${encodeURIComponent(patientUsername)}&doctor=${encodeURIComponent(doctorName)}&date=${encodeURIComponent(date)}&time=${encodeURIComponent(time)}`;

  // ✨ Use userName (full name) in email content now
  const mailOptions = {
    from: '"ZimbaLife Health App" <ssenkubugeabbey055@gmail.com>',
    to: doctorEmail,
    subject: `New Appointment Request from ${userName}`,  // <-- full name here
    html: `
      <p>Dear ${doctorName},</p>
      <p>You have a new appointment request from <strong>${userName}</strong>.</p>
      <p><strong>Specialization:</strong> ${specialization}</p>
      <p><strong>Date:</strong> ${date}</p>
      <p><strong>Time:</strong> ${time}</p>
      <p>Please confirm this appointment by clicking the button below:</p>
      <p>
        <a href="${confirmationLink}" style="
          background-color:#4CAF50;
          color:white;
          padding:10px 20px;
          text-decoration:none;
          border-radius:5px;
          display:inline-block;
        ">Confirm Appointment</a>
      </p>
      <br>
      <p>Thank you,<br>ZimbaLife Health App</p>
    `,
  };

  try {
    await transporter.sendMail(mailOptions);
    logger.info("Email sent to", doctorEmail);
    res.status(200).json({ message: "Appointment email sent successfully!" });
  } catch (error) {
    logger.error("Email error:", error);
    res.status(500).json({ error: "Failed to send appointment email." });
  }
});

// GET /confirm => Doctor confirms the appointment
app.get("/confirm", async (req, res) => {
  const { user, doctor, date, time } = req.query;

  if (!user || !doctor || !date || !time) {
    return res.status(400).send("Missing confirmation data.");
  }

  const patientKey = user.trim().toLowerCase();

  logger.info("Appointment confirmation received", {
    patientKey,
    doctor,
    date,
    time,
  });

  try {
    // Save confirmed appointment
    await db.ref("confirmedAppointments").push({
      patientName: patientKey,
      doctorName: doctor,
      date,
      time,
      confirmedAt: new Date().toISOString(),
    });

    // Send notification to patient
    await db.ref("Notifications").child(patientKey).push({
      title: "Appointment Confirmed",
      message: `Your appointment with Dr. ${doctor} on ${date} at ${time} has been confirmed.`,
      patientName: patientKey,
      timestamp: new Date().toISOString(),
    });

    // Send success HTML
    res.send(`
      <html>
        <head><title>Appointment Confirmed</title></head>
        <body style="text-align:center; font-family:sans-serif; padding:40px;">
          <h2>✅ Appointment Confirmed</h2>
          <p>Thank you, ${doctor}, for confirming your appointment with ${patientKey} on ${date} at ${time}.</p>
        </body>
      </html>
    `);
  } catch (error) {
    logger.error("Confirmation error:", error);
    res.status(500).send("Something went wrong while saving confirmation.");
  }
});

// Export the Cloud Function
exports.confirmAppointment = onRequest(app);
