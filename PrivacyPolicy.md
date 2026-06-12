# Privacy Policy

**Effective Date:** June 12, 2026

danycli built the Assignly Desktop application as a free utility. This SERVICE is provided by danycli at no cost and is intended for use as is.

This page informs users regarding our policies concerning the collection, use, and disclosure of Personal Information for anyone using the Assignly Desktop application.

By using Assignly Desktop, you agree to the collection and use of information in accordance with this policy.

## Information Collection and Use

Assignly Desktop is a custom, offline-first client designed exclusively to interface with the COMSATS University Islamabad, Abbottabad Campus student portal (`sis.cuiatd.edu.pk`). To function properly, the app requires you to provide your university portal credentials (Registration Number and Password).

### How Your Data Is Handled

* **Direct Connection:** The application communicates directly with the university's portal securely. Your credentials, academic data (assignments, deadlines, attendance insights, financial history, and profile information) are never routed through, collected by, or stored on any intermediate third-party servers.

* **Secure Local Storage:** To keep you logged in and enable the offline-first experience, your credentials and scraped academic data are saved locally on your computer's hard drive. Your password is encrypted using robust AES-256 encryption (`EncryptionUtil.java`) before being saved, ensuring it cannot be easily read by other applications or scripts.

* **Offline Data Caching:** Assignly Desktop utilizes a local SQLite database (`DatabaseManager.java`) to cache snapshots of your portal data. This allows you to view your schedules, assignments, and grades even when you have no active internet connection. This database remains strictly on your local machine.

* **File Handling:** When you upload assignment files or download resources (like lecture slides or fee challans), the transfer occurs directly between your computer's local storage and the university portal.

## Log Data and Analytics

Assignly Desktop respects your privacy and does not include any third-party tracking, analytics, or background crash-reporting software. The application does not collect background log data, track your desktop usage habits, or monetize your activity.

## Cookies

Cookies are files with a small amount of data that are commonly used as anonymous unique identifiers. Assignly Desktop uses cookies strictly to pass security verifications and maintain your active session with the university portal. These cookies are stored securely within the app's internal HTTP client (OkHttp) and are cleared when your session expires or when you explicitly log out of the application.

## Security

We value your trust in providing your credentials, which is why we enforce strict local encryption. All network requests made by the app are transmitted over secure HTTPS connections directly to the official portal. However, please note that no method of transmission over the internet or method of electronic storage is 100% secure, and absolute security cannot be guaranteed. We strongly recommend protecting your computer with a secure login password and ensuring your local file system is safe from malicious software.

## External Services

This Service interfaces directly with the COMSATS University portal. While the application facilitates seamless access to your academic data and assignment submissions, it is an independent tool and is not officially affiliated with the COMSATS University administration. We strongly advise you to review the Privacy Policy of the university itself, as we have no control over and assume no responsibility for the content, privacy policies, or practices of the official portal.

## Changes to This Privacy Policy

We may update our Privacy Policy from time to time as new features are added to the desktop application. You are advised to review this page periodically for any changes.

## Contact Us

If you have any questions, concerns, or suggestions about this Privacy Policy, please do not hesitate to reach out by opening an issue on the Assignly Desktop GitHub repository.
