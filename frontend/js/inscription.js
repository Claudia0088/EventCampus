
const API_BASE_URL =
    `http://${window.location.hostname}:8080/api/auth`;

const registerForm = document.getElementById("registerForm");
const registerMessage = document.getElementById("registerMessage");
const registerButton = document.getElementById("registerButton");

if (registerForm && registerMessage && registerButton) {
    registerForm.addEventListener("submit", async function (event) {
        event.preventDefault();

        const firstName = document
            .getElementById("firstName")
            .value.trim();

        const lastName = document
            .getElementById("lastName")
            .value.trim();

        const email = document
            .getElementById("email")
            .value.trim();

        const password = document.getElementById("password").value;
        const confirmPassword =
            document.getElementById("confirmPassword").value;

        registerMessage.textContent = "";

        // Vérification des champs obligatoires
        if (!firstName || !lastName || !email) {
            registerMessage.textContent =
                "Veuillez remplir tous les champs obligatoires.";
            return;
        }

        // Vérification du mot de passe
        if (password.length < 8) {
            registerMessage.textContent =
                "Le mot de passe doit contenir au moins 8 caractères.";
            return;
        }

        // Vérification de la confirmation
        if (password !== confirmPassword) {
            registerMessage.textContent =
                "Les deux mots de passe ne correspondent pas.";
            return;
        }

        registerButton.disabled = true;
        registerMessage.textContent =
            "Création du compte en cours...";

        try {
            const response = await fetch(
                `${API_BASE_URL}/register`,
                {
                    method: "POST",
                    headers: {
                        "Content-Type": "application/json",
                        "Accept": "application/json"
                    },
                    body: JSON.stringify({
                        firstName: firstName,
                        lastName: lastName,
                        email: email,
                        password: password
                    })
                }
            );

            const responseBody = await response.text();
            let result;

            try {
                result = responseBody
                    ? JSON.parse(responseBody)
                    : {};
            } catch {
                result = { message: responseBody };
            }

            if (response.ok) {
                registerMessage.textContent =
                    `${result.message || "Compte créé avec succès"} ! Redirection vers la connexion...`;

                registerForm.reset();

                setTimeout(function () {
                    window.location.href = "connexion.html";
                }, 1500);
            } else {
                registerMessage.textContent =
                    result.message ||
                    result.detail ||
                    "Impossible de créer le compte.";
            }

        } catch (error) {
            console.error("Erreur d'inscription :", error);

            registerMessage.textContent =
                "Impossible de contacter le serveur. Vérifiez que le backend fonctionne.";
        } finally {
            registerButton.disabled = false;
        }
    });
} else {
    console.error(
        "Le formulaire d'inscription ou ses éléments sont introuvables."
    );
}
