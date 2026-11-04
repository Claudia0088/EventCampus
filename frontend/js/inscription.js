
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
                "http://localhost:8080/api/auth/register",
                {
                    method: "POST",
                    headers: {
                        "Content-Type": "application/json"
                    },
                    body: JSON.stringify({
                        firstName: firstName,
                        lastName: lastName,
                        email: email,
                        password: password
                    })
                }
            );

            const message = await response.text();

            if (response.ok) {
                registerMessage.textContent =
                    "Compte créé avec succès ! Redirection vers la connexion...";

                registerForm.reset();

                setTimeout(function () {
                    window.location.href = "connexion.html";
                }, 1500);
            } else {
                registerMessage.textContent =
                    message || "Impossible de créer le compte.";
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
