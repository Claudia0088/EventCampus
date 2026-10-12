const API_BASE_URL =
    `http://${window.location.hostname}:8080/api/auth`;

const loginForm = document.getElementById("loginForm");
const loginMessage = document.getElementById("loginMessage");

// Redirection selon le rôle de l'utilisateur.
function obtenirPageSelonRole(role) {
    const roleNormalise = String(role || "")
        .trim()
        .toUpperCase();

    const pages = {
        USER: "espace-user.html",
        MODERATOR: "espace-moderator.html",
        ADMIN: "espace-admin.html"
    };

    return pages[roleNormalise] || null;
}

if (!loginForm || !loginMessage) {
    console.error(
        "Formulaire de connexion ou message introuvable."
    );
} else {
    loginForm.addEventListener("submit", async function (event) {
        event.preventDefault();

        const email =
            document.getElementById("email").value.trim();

        const password =
            document.getElementById("password").value;

        loginMessage.textContent = "Connexion en cours...";

        try {
            const response = await fetch(
                `${API_BASE_URL}/login`,
                {
                    method: "POST",
                    credentials: "include",
                    headers: {
                        "Content-Type": "application/json",
                        "Accept": "application/json"
                    },
                    body: JSON.stringify({
                        email: email,
                        password: password
                    })
                }
            );

            const result = await response.json();

            if (!response.ok) {
                loginMessage.textContent =
                    result.message ||
                    "La connexion a échoué.";
                return;
            }

            const page = obtenirPageSelonRole(result.role);

            if (!page) {
                loginMessage.textContent =
                    "Le rôle de ce compte est inconnu.";
                return;
            }

            localStorage.setItem("loggedIn", "true");
            localStorage.setItem(
                "userEmail",
                result.email || email
            );

            loginMessage.textContent =
                "Connexion réussie ! Redirection...";

            window.location.href = new URL(
                page,
                window.location.href
            ).href;

        } catch (error) {
            console.error(
                "Erreur de connexion :",
                error
            );

            loginMessage.textContent =
                "Impossible de communiquer avec le serveur.";
        }
    });
}