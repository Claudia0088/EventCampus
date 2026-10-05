const loggedIn = localStorage.getItem("loggedIn");
const userEmail = localStorage.getItem("userEmail");

const userEmailElement = document.getElementById("userEmail");
const logoutButton = document.getElementById("logoutButton");


if (loggedIn !== "true") {

    window.location.href = "connexion.html";

}


if (userEmail) {

    userEmailElement.textContent =
        "Connecté avec : " + userEmail;

}


logoutButton.addEventListener("click", function() {

    localStorage.removeItem("loggedIn");
    localStorage.removeItem("userEmail");

    window.location.href = "../index.html";

});