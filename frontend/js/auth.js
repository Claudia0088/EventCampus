const loginForm = document.getElementById("loginForm");
const loginMessage = document.getElementById("loginMessage");

loginForm.addEventListener("submit", function(event) {

    event.preventDefault();

    loginMessage.textContent = "Le bouton fonctionne !";

});