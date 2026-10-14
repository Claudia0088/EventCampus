
/* =====================================================
   EVENTCAMPUS — ESPACE PERSONNEL
   Session, navigation, menu mobile et filtres
   ===================================================== */

/* 1. CONFIGURATION */

const API_BASE_URL =
    `http://${window.location.hostname}:8080/api/auth`;


/* 2. ÉLÉMENTS DU PROFIL */

const userEmailElement = document.getElementById("userEmail");
const logoutButton = document.getElementById("logoutButton");

const welcomeName = document.getElementById("welcomeName");
const welcomeDescription =
    document.getElementById("welcomeDescription");

const userFullName = document.getElementById("userFullName");
const userRoleBadge = document.getElementById("userRoleBadge");

const sidebarUserName =
    document.getElementById("sidebarUserName");

const sidebarUserRole =
    document.getElementById("sidebarUserRole");

const userAvatar = document.getElementById("userAvatar");
const profileAvatar = document.getElementById("profileAvatar");

const dashboardAlert =
    document.getElementById("dashboardAlert");


/* 3. NAVIGATION SELON LE RÔLE */

const moderatorNavigation =
    document.getElementById("moderatorNavigation");

const adminNavigation =
    document.getElementById("adminNavigation");

const moderationAction =
    document.getElementById("moderationAction");

const administrationAction =
    document.getElementById("administrationAction");


/* 4. MENU MOBILE */

const menuToggle = document.getElementById("menuToggle");
const sidebar = document.getElementById("sidebar");

const sidebarOverlay =
    document.getElementById("sidebarOverlay");


/* 5. AUTRES ÉLÉMENTS */

const currentYear = document.getElementById("currentYear");

const eventSearch = document.getElementById("eventSearch");

const eventCategoryFilter =
    document.getElementById("eventCategoryFilter");

const eventsContainer =
    document.getElementById("eventsContainer");


/* 6. ANNÉE DU PIED DE PAGE */

if (currentYear) {
    currentYear.textContent = new Date().getFullYear();
}


/* 7. AFFICHAGE DES MESSAGES */

function afficherErreur(message) {
    if (!dashboardAlert) {
        console.error(message);
        return;
    }

    dashboardAlert.textContent = message;
    dashboardAlert.hidden = false;

    dashboardAlert.setAttribute("role", "alert");
    dashboardAlert.classList.add("alert", "alert-error");
}

function masquerMessage() {
    if (!dashboardAlert) {
        return;
    }

    dashboardAlert.textContent = "";
    dashboardAlert.hidden = true;
    dashboardAlert.classList.remove(
        "alert",
        "alert-error",
        "alert-success",
        "alert-info"
    );
}


/* 8. INITIALLES DU PROFIL */

function initiale(prenom, nom) {
    const texte = `${prenom || ""} ${nom || ""}`.trim();

    return texte ? texte.charAt(0).toUpperCase() : "E";
}


/* 9. MENU MOBILE */

function ouvrirMenu() {
    if (sidebar) {
        sidebar.classList.add("sidebar-open");
    }

    if (menuToggle) {
        menuToggle.setAttribute("aria-expanded", "true");
    }

    if (sidebarOverlay) {
        sidebarOverlay.hidden = false;
        sidebarOverlay.classList.add("active");
    }

    document.body.style.overflow = "hidden";
}

function fermerMenu() {
    if (sidebar) {
        sidebar.classList.remove("sidebar-open");
    }

    if (menuToggle) {
        menuToggle.setAttribute("aria-expanded", "false");
    }

    if (sidebarOverlay) {
        sidebarOverlay.classList.remove("active");
        sidebarOverlay.hidden = true;
    }

    document.body.style.overflow = "";
}

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

if (sidebarOverlay) {
    sidebarOverlay.addEventListener("click", fermerMenu);
}

document.addEventListener("keydown", function (event) {
    if (event.key === "Escape") {
        fermerMenu();
    }
});

window.addEventListener("resize", function () {
    if (window.innerWidth > 850) {
        fermerMenu();
    }
});


/* 10. NAVIGATION ENTRE LES RUBRIQUES */

function configurerLiensNavigation() {
    const liens = document.querySelectorAll(
        ".sidebar-nav a[href^='#']"
    );

    function actualiserLienActif() {
        const hash = window.location.hash || "#accueil";

        liens.forEach(function (link) {
            const estActif = link.getAttribute("href") === hash;

            link.classList.toggle("active", estActif);

            if (estActif) {
                link.setAttribute("aria-current", "page");
            } else {
                link.removeAttribute("aria-current");
            }
        });
    }

    liens.forEach(function (link) {
        link.addEventListener("click", function () {
            fermerMenu();

            const cible = document.querySelector(
                link.getAttribute("href")
            );

            if (cible) {
                cible.scrollIntoView({
                    behavior: "smooth",
                    block: "start"
                });
            }
        });
    });

    window.addEventListener("hashchange", actualiserLienActif);

    actualiserLienActif();
}

configurerLiensNavigation();


/* 11. AFFICHAGE SELON LE RÔLE */

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

    const registrationsNav =
        document.getElementById("registrationsNav");

    // Le rôle USER conserve l'accès à ses inscriptions.
    // Le masquage ici concerne l'interface, pas la sécurité serveur.
    if (registrationsNav && roleNormalise === "MODERATOR") {
        registrationsNav.hidden = true;
    }
}


/* 12. VÉRIFICATION DE LA SESSION */

async function verifierSession() {
    try {
        const response = await fetch(`${API_BASE_URL}/me`, {
            method: "GET",
            credentials: "include",
            headers: {
                Accept: "application/json"
            }
        });

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

        const roleAttendu =
            document.documentElement.dataset.requiredRole;

        const rolesAutorises = ["USER", "MODERATOR", "ADMIN"];

        if (!rolesAutorises.includes(role)) {
            afficherErreur(
                "Le rôle de ce compte est inconnu. Contacte l'administration."
            );
            return;
        }

        // Redirection si le compte n'utilise pas le bon espace.
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
        masquerMessage();

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


/* 13. RECHERCHE D'ÉVÉNEMENTS */

function filtrerEvenements() {
    if (!eventsContainer) {
        return;
    }

    const terme = eventSearch
        ? eventSearch.value.trim().toLocaleLowerCase("fr")
        : "";

    const categorieChoisie = eventCategoryFilter
        ? eventCategoryFilter.value.trim().toLocaleLowerCase("fr")
        : "";

    const cartes = eventsContainer.querySelectorAll(".event-card");

    let nombreVisible = 0;

    cartes.forEach(function (carte) {
        const texteCarte = carte.textContent
            .toLocaleLowerCase("fr");

        const categorieElement =
            carte.querySelector(".event-category");

        const categorieCarte = categorieElement
            ? categorieElement.textContent
                .trim()
                .toLocaleLowerCase("fr")
            : "";

        const correspondAuTexte =
            !terme || texteCarte.includes(terme);

        const correspondALaCategorie =
            !categorieChoisie ||
            categorieChoisie === "all" ||
            categorieCarte === categorieChoisie ||
            categorieCarte.includes(categorieChoisie);

        const estVisible =
            correspondAuTexte && correspondALaCategorie;

        carte.hidden = !estVisible;

        if (estVisible) {
            nombreVisible++;
        }
    });

    let messageVide = eventsContainer.querySelector(
        ".no-events-message"
    );

    if (nombreVisible === 0) {
        if (!messageVide) {
            messageVide = document.createElement("p");
            messageVide.className = "no-events-message";
            messageVide.textContent =
                "Aucun événement ne correspond à ta recherche.";

            messageVide.style.gridColumn = "1 / -1";
            messageVide.style.padding = "24px";
            messageVide.style.textAlign = "center";
            messageVide.style.color = "#64748b";

            eventsContainer.appendChild(messageVide);
        }
    } else if (messageVide) {
        messageVide.remove();
    }
}

if (eventSearch) {
    eventSearch.addEventListener("input", filtrerEvenements);
}

if (eventCategoryFilter) {
    eventCategoryFilter.addEventListener(
        "change",
        filtrerEvenements
    );
}


/* 14. DÉCONNEXION */

if (logoutButton) {
    logoutButton.addEventListener("click", async function () {
        if (logoutButton.disabled) {
            return;
        }

        logoutButton.disabled = true;

        const ancienTexte = logoutButton.textContent;
        logoutButton.textContent = "Déconnexion en cours...";

        try {
            const response = await fetch(
                `${API_BASE_URL}/logout`,
                {
                    method: "POST",
                    credentials: "include",
                    headers: {
                        Accept: "application/json"
                    }
                }
            );

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


/* 15. INITIALISATION */

filtrerEvenements();
verifierSession();
