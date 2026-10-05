const loginForm = document.getElementById("loginForm");
const loginMessage = document.getElementById("loginMessage");

loginForm.addEventListener("submit", async function(event) {

    event.preventDefault();

    const email = document.getElementById("email").value;
    const password = document.getElementById("password").value;

    loginMessage.textContent = "Connexion en cours...";

    try {

        const response = await fetch("http://localhost:8080/api/auth/login", {
            method: "POST",

            headers: {
                "Content-Type": "application/json"
            },

            body: JSON.stringify({
                email: email,
                password: password
            })
        });

        const result = await response.text();

        if (result === "Connexion réussie") {

            loginMessage.textContent = "Connexion réussie !";

            localStorage.setItem("loggedIn", "true");
            localStorage.setItem("userEmail", email);

            setTimeout(function() {
                window.location.href = "mon-espace.html";
            }, 1000);

        } else {

            loginMessage.textContent = result;

        }

    } catch (error) {

        console.error(error);

        loginMessage.textContent =
            "Impossible de contacter le serveur.";

    }

});