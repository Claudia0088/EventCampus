const API_BASE_URL =
    `http://${window.location.hostname}:8080/api/auth`;

// Éléments du profil
const userEmailElement = document.getElementById("userEmail");
const logoutButton = document.getElementById("logoutButton");

const welcomeName = document.getElementById("welcomeName");
const welcomeDescription = document.getElementById("welcomeDescription");
const userFullName = document.getElementById("userFullName");
const userRoleBadge = document.getElementById("userRoleBadge");
const sidebarUserName = document.getElementById("sidebarUserName");
const sidebarUserRole = document.getElementById("sidebarUserRole");
const userAvatar = document.getElementById("userAvatar");
const profileAvatar = document.getElementById("profileAvatar");
const dashboardAlert = document.getElementById("dashboardAlert");

// Navigation par rôle
const moderatorNavigation =
    document.getElementById("moderatorNavigation");
const adminNavigation =
    document.getElementById("adminNavigation");
const moderationAction =
    document.getElementById("moderationAction");
const administrationAction =
    document.getElementById("administrationAction");

// Menu mobile
const menuToggle = document.getElementById("menuToggle");
const sidebar = document.getElementById("sidebar");
const sidebarOverlay = document.getElementById("sidebarOverlay");

// Année du pied de page
const currentYear = document.getElementById("currentYear");

if (currentYear) {
    currentYear.textContent = new Date().getFullYear();
}


// Afficher un message d'erreur
function afficherErreur(message) {
    if (!dashboardAlert) return;

    dashboardAlert.textContent = message;
    dashboardAlert.hidden = false;
}


// Obtenir l'initiale du prénom et du nom
function initiale(prenom, nom) {
    const texte = `${prenom || ""} ${nom || ""}`.trim();

    return texte ? texte.charAt(0).toUpperCase() : "E";
}


// Fermer le menu mobile
function fermerMenu() {
    sidebar?.classList.remove("sidebar-open");

    if (menuToggle) {
        menuToggle.setAttribute("aria-expanded", "false");
    }

    if (sidebarOverlay) {
        sidebarOverlay.hidden = true;
    }
}


// Ouvrir le menu mobile
function ouvrirMenu() {
    sidebar?.classList.add("sidebar-open");

    if (menuToggle) {
        menuToggle.setAttribute("aria-expanded", "true");
    }

    if (sidebarOverlay) {
        sidebarOverlay.hidden = false;
    }
}


// Configurer l'affichage selon le rôle
function configurerNavigation(role) {
    const roleNormalise = String(role || "")
        .trim()
        .toUpperCase();

    const estModerateur =
        roleNormalise === "MODERATOR" ||
        roleNormalise === "ADMIN";

    const estAdministrateur =
        roleNormalise === "ADMIN";

    if (moderatorNavigation) {
        moderatorNavigation.hidden = !estModerateur;
    }

    if (adminNavigation) {
        adminNavigation.hidden = !estAdministrateur;
    }

    if (moderationAction) {
        moderationAction.hidden = !estModerateur;
    }

    if (administrationAction) {
        administrationAction.hidden = !estAdministrateur;
    }

    const nomsRoles = {
        USER: "Étudiant",
        MODERATOR: "Modérateur",
        ADMIN: "Administrateur"
    };

    if (userRoleBadge) {
        userRoleBadge.textContent =
            nomsRoles[roleNormalise] || "Utilisateur";
    }

    const descriptionsRoles = {
        USER: "Membre du campus",
        MODERATOR: "Équipe de modération",
        ADMIN: "Administration"
    };

    if (sidebarUserRole) {
        sidebarUserRole.textContent =
            descriptionsRoles[roleNormalise] ||
            "Membre EventCampus";
    }

    const descriptionsBienvenue = {
        USER:
            "Découvre les activités du campus, rencontre de nouvelles personnes et prépare tes prochaines sorties.",
        MODERATOR:
            "Retrouve ton espace de modération et contribue à une communauté étudiante accueillante.",
        ADMIN:
            "Retrouve ton espace d'administration pour superviser la plateforme EventCampus."
    };

    if (welcomeDescription) {
        welcomeDescription.textContent =
            descriptionsBienvenue[roleNormalise] ||
            "Bienvenue dans ton espace personnel EventCampus.";
    }

    // Masquer les inscriptions sur l'espace modérateur.
    if (roleNormalise === "MODERATOR") {
        const registrationsNav =
            document.getElementById("registrationsNav");

        if (registrationsNav) {
            registrationsNav.hidden = true;
        }
    }
}


// Vérifier la session et le rôle réel
async function verifierSession() {
    try {
        const response = await fetch(`${API_BASE_URL}/me`, {
            method: "GET",
            credentials: "include",
            headers: {
                Accept: "application/json"
            }
        });

        // Utilisateur non connecté
        if (response.status === 401 || response.status === 403) {
            localStorage.removeItem("loggedIn");
            localStorage.removeItem("userEmail");

            window.location.replace("connexion.html");
            return;
        }

        if (!response.ok) {
            afficherErreur(
                "Impossible de charger ton profil. Réessaie dans quelques instants."
            );
            return;
        }

        const user = await response.json();

        const prenom = user.firstName || "";
        const nom = user.lastName || "";
        const email = user.email || "";

        const role = String(user.role || "")
            .trim()
            .toUpperCase();

        // Vérifier que le rôle correspond à la page ouverte.
        const roleAttendu =
            document.documentElement.dataset.requiredRole;

        if (!["USER", "MODERATOR", "ADMIN"].includes(role)) {
            afficherErreur(
                "Le rôle de ce compte est inconnu. Contacte l'administration."
            );
            return;
        }

        if (roleAttendu && role !== roleAttendu) {
            const pagesSelonRole = {
                USER: "espace-user.html",
                MODERATOR: "espace-moderator.html",
                ADMIN: "espace-admin.html"
            };

            const page = pagesSelonRole[role];

            if (page) {
                window.location.replace(page);
            } else {
                window.location.replace("connexion.html");
            }

            return;
        }

        if (!email) {
            afficherErreur(
                "L'adresse e-mail du compte est introuvable."
            );
            return;
        }

        localStorage.setItem("loggedIn", "true");
        localStorage.setItem("userEmail", email);

        const nomComplet =
            `${prenom} ${nom}`.trim() || email;

        if (welcomeName) {
            welcomeName.textContent = prenom || nomComplet;
        }

        if (userFullName) {
            userFullName.textContent = nomComplet;
        }

        if (userEmailElement) {
            userEmailElement.textContent = email;
        }

        if (sidebarUserName) {
            sidebarUserName.textContent = nomComplet;
        }

        const lettre = initiale(prenom, nom);

        if (userAvatar) {
            userAvatar.textContent = lettre;
        }

        if (profileAvatar) {
            profileAvatar.textContent = lettre;
        }

        configurerNavigation(role);

    } catch (error) {
        console.error(
            "Erreur de vérification de session :",
            error
        );

        afficherErreur(
            "Le serveur est inaccessible. Vérifie que Spring Boot est démarré."
        );
    }
}


// Bouton du menu mobile
if (menuToggle) {
    menuToggle.addEventListener("click", function () {
        const menuOuvert =
            sidebar?.classList.contains("sidebar-open");

        if (menuOuvert) {
            fermerMenu();
        } else {
            ouvrirMenu();
        }
    });
}


// Fermer le menu en cliquant sur le fond
if (sidebarOverlay) {
    sidebarOverlay.addEventListener("click", fermerMenu);
}


// Fermer le menu après avoir choisi une rubrique
document.querySelectorAll(".sidebar-nav a").forEach(function (link) {
    link.addEventListener("click", fermerMenu);
});


// Déconnexion
if (logoutButton) {
    logoutButton.addEventListener("click", async function () {
        logoutButton.disabled = true;
        logoutButton.textContent = "Déconnexion en cours...";

        try {
            const response = await fetch(`${API_BASE_URL}/logout`, {
                method: "POST",
                credentials: "include",
                headers: {
                    Accept: "application/json"
                }
            });

            if (!response.ok) {
                console.error(
                    "Le serveur n'a pas confirmé la déconnexion."
                );
            }

        } catch (error) {
            console.error(
                "Erreur lors de la déconnexion :",
                error
            );

        } finally {
            localStorage.removeItem("loggedIn");
            localStorage.removeItem("userEmail");

            window.location.replace("../index.html");
        }
    });
}


// Mettre en évidence la rubrique sélectionnée
document.querySelectorAll(".sidebar-nav a[href^='#']").forEach(function (link) {
    link.addEventListener("click", function () {
        document.querySelectorAll(".sidebar-nav a").forEach(function (item) {
            item.classList.remove("active");
        });

        link.classList.add("active");
    });
});


// Charger le profil depuis Spring Boot
verifierSession();