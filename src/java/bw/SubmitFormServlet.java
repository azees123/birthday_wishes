package bw;

import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.*;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.*;
import java.sql.Date;

public class SubmitFormServlet extends HttpServlet {

    private static final String FROM_EMAIL = "azees2663@gmail.com"; // Your email
    private static final String PASSWORD = "fcdq agll zjra xdap"; // App password

    // Static block to explicitly load MySQL JDBC driver
    static {
        try {
            // Explicitly load MySQL JDBC driver
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("MySQL JDBC Driver not found. Ensure the library is included in the project.", e);
        }
    }

    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String name = request.getParameter("name");
        String email = request.getParameter("email");
        String dob = request.getParameter("dob");
        String time = request.getParameter("time");

        // Step 1: Save details to the database
        saveUserDetails(name, email, dob, time);

        // Step 2: Schedule the email to be sent based on the user's DOB and time
        scheduleEmail(name, email, dob, time);

        // Step 3: Respond to user
        response.setContentType("text/html");
        PrintWriter out = response.getWriter();
        out.println("<h3>Registration successful!</h3>");
        out.println("<h3>We'll send you an email on your birthday at the scheduled time.</h3>");
        out.println("<script>");
        out.println("setTimeout(function() { window.location.href = 'index.html'; }, 3000);");  // Redirect after 3 seconds
        out.println("</script>");
    }

    private void saveUserDetails(String name, String email, String dob, String time) {
        String dbURL = "jdbc:mysql://localhost:3306/user_info";
        String dbUsername = "root"; // MySQL username
        String dbPassword = ""; // MySQL password

        try {
            // Ensure time is in the correct format (HH:MM:SS)
            if (time != null && time.length() == 5) {
                time += ":00";  // Add seconds if not present
            }

            // Validate that the time is now in the correct format (HH:MM:SS)
            if (time != null && time.matches("\\d{2}:\\d{2}:\\d{2}")) {
                try (Connection conn = DriverManager.getConnection(dbURL, dbUsername, dbPassword)) {
                    String sql = "INSERT INTO user_details (name, email, dob, time, birthday_email_sent) VALUES (?, ?, ?, ?, ?)";
                    try (PreparedStatement statement = conn.prepareStatement(sql)) {
                        statement.setString(1, name);
                        statement.setString(2, email);
                        statement.setDate(3, Date.valueOf(dob));
                        statement.setTime(4, Time.valueOf(time));  // This should now work if time is valid
                        statement.setInt(5, 0);  // Initially, birthday email has not been sent (0).
                        statement.executeUpdate();
                    }
                }
            } else {
                throw new IllegalArgumentException("Time format is invalid. Expected format: HH:MM:SS");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            e.printStackTrace(); // Log or handle invalid time format
        }
    }

    private void scheduleEmail(String name, String email, String dob, String time) {
        // Convert the time string into LocalTime for easier manipulation
        LocalTime scheduledTime = LocalTime.parse(time);

        // Get today's date and the user's birthday (ignoring the year)
        LocalDate today = LocalDate.now();
        LocalDate birthday = LocalDate.parse(dob);

        // If it's the user's birthday today, schedule the email to send at the scheduled time
        if (today.getMonth() == birthday.getMonth() && today.getDayOfMonth() == birthday.getDayOfMonth()) {
            long delay = calculateDelay(scheduledTime, today); // Delay until the scheduled time today or tomorrow

            // Schedule the task
            ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
            scheduler.schedule(() -> sendBirthdayEmail(name, email), delay, TimeUnit.MILLISECONDS);
        }
    }

    private long calculateDelay(LocalTime scheduledTime, LocalDate today) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime scheduledDateTime = LocalDateTime.of(today, scheduledTime);

        // If the scheduled time is in the future today
        if (scheduledDateTime.isAfter(now)) {
            return java.time.Duration.between(now, scheduledDateTime).toMillis(); // Today's delay
        } else {
            // If the scheduled time has passed today, schedule for the next day
            scheduledDateTime = scheduledDateTime.plusDays(1);
            return java.time.Duration.between(now, scheduledDateTime).toMillis(); // Next day's delay
        }
    }

    private void sendBirthdayEmail(String name, String email) {
        String subject = "Happy Birthday!";
        String message = "Dear " + name + ",\n\n Happy Birthday! " + name + ", We hope you have a wonderful day!\n\nBest wishes!";
        sendEmail(email, subject, message);
    }

    private void sendEmail(String to, String subject, String message) {
        Properties props = new Properties();
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");

        Session session = Session.getInstance(props, new jakarta.mail.Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(FROM_EMAIL, PASSWORD);
            }
        });

        try {
            Message msg = new MimeMessage(session);
            msg.setFrom(new InternetAddress(FROM_EMAIL));
            msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
            msg.setSubject(subject);
            msg.setText(message);

            Transport.send(msg);
        } catch (MessagingException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    @Override
    public String getServletInfo() {
        return "User Registration Servlet";
    }
}
