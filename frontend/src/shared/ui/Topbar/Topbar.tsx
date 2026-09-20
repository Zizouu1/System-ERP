import { useEffect, useMemo, useRef, useState } from "react";
import { useLocation, useNavigate } from "react-router-dom";
import { useAuth } from "../../../app/providers/AuthProvider";
import xLogo from "@/assets/X_white.png";
import notificationIcon from "@/assets/notification.png";
import {
  adminListNotifications,
  adminMarkAllNotificationsSeen,
  adminMarkNotificationSeen,
  adminUnreadNotificationsCount,
} from "@/shared/api/endpoints";
import type { AdminNotificationDto } from "@/shared/api/types";
import "./topbar.css";

export default function Topbar() {
  const { user } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [searchTerm, setSearchTerm] = useState("");
  const [searchResults, setSearchResults] = useState<
    Array<{ label: string; to: string }>
  >([]);
  const [notifOpen, setNotifOpen] = useState(false);
  const [notifications, setNotifications] = useState<AdminNotificationDto[]>(
    [],
  );
  const [notifBusy, setNotifBusy] = useState(false);
  const [unreadCount, setUnreadCount] = useState(0);
  const notifLoadSeqRef = useRef(0);

  const isAdmin = user?.role === "admin";

  const visibleNotifications = useMemo(
    () => notifications.slice(0, 12),
    [notifications],
  );

  async function loadNotifications(source = "unknown") {
    if (!isAdmin) return;
    const seq = ++notifLoadSeqRef.current;
    console.debug("[notif-ui] loadNotifications:start", { source, seq });

    try {
      const [rows, countResp] = await Promise.all([
        adminListNotifications(),
        adminUnreadNotificationsCount(),
      ]);

      if (seq !== notifLoadSeqRef.current) {
        console.debug("[notif-ui] loadNotifications:staleIgnored", {
          source,
          seq,
          latest: notifLoadSeqRef.current,
        });
        return;
      }

      console.debug("[notif-ui] loadNotifications:response", {
        source,
        seq,
        unreadCountFromApi: countResp.count ?? 0,
        firstRows: rows.slice(0, 5).map((r) => ({ id: r.id, seen: r.seen })),
      });

      setNotifications(rows);
      setUnreadCount(countResp.count ?? 0);
    } catch (error) {
      console.error("[notif-ui] loadNotifications:error", {
        source,
        seq,
        error,
      });
    }
  }

  useEffect(() => {
    if (!isAdmin) return;

    loadNotifications("initial");

    // disable for debugging
    // const timer = window.setInterval(() => loadNotifications("poll"), 15000);
    // return () => window.clearInterval(timer);

    return undefined;
  }, [isAdmin]);

  const allRoutes = [
    { label: "Tableau de bord", to: "/dashboard", roles: ["admin"] },
    { label: "Produits", to: "/products", roles: ["admin"] },
    { label: "Utilisateurs", to: "/users", roles: ["admin"] },
    { label: "Employes", to: "/employees", roles: ["admin"] },
    { label: "Nomenclature", to: "/nomenclature", roles: ["admin"] },
    { label: "Stock", to: "/stock", roles: ["admin"] },
    { label: "Simulation", to: "/simulation", roles: ["admin"] },
    {
      label: "Retard de Production",
      to: "/prediction-retard-production",
      roles: ["admin"],
    },
    {
      label: "Stock département 1",
      to: "/departement-1/stock",
      roles: ["admin", "logistic"],
    },
    {
      label: "Production",
      to: "/departement-1/production",
      roles: ["admin", "psf"],
    },
    {
      label: "Entrées",
      to: "/departement-1/incoming",
      roles: ["admin", "logistic"],
    },
    {
      label: "Sorties logistique",
      to: "/logistic/outgoing",
      roles: ["admin", "logistic"],
    },
    {
      label: "Sorties PSF",
      to: "/psf/outgoing",
      roles: ["admin", "psf"],
    },
    { label: "Département 2", to: "/departement-2", roles: ["admin", "pf"] },
    { label: "Export", to: "/export", roles: ["admin"] },
    {
      label: "Nouvelle Production",
      to: "/production",
      roles: ["pf"],
    },
  ];

  const filteredRoutes = allRoutes.filter((r) =>
    r.roles.includes(user?.role || ""),
  );

  const handleSearch = (val: string) => {
    setSearchTerm(val);
    if (!val.trim()) {
      setSearchResults([]);
      return;
    }
    const matches = filteredRoutes.filter((r) =>
      r.label.toLowerCase().includes(val.toLowerCase()),
    );
    setSearchResults(matches);
  };

  const handleResultClick = (to: string) => {
    navigate(to);
    setSearchTerm("");
    setSearchResults([]);
  };

  async function handleNotificationClick(notification: AdminNotificationDto) {
    if (!Number.isFinite(notification.id) || notification.id <= 0) {
      console.error("[notif-ui] click:invalid-id", { id: notification.id });
      return;
    }

    const wasSeen = Boolean(notification.seen);
    console.info("[notif-ui] click", {
      id: notification.id,
      seenBefore: wasSeen,
      targetPath: notification.targetPath,
    });

    if (!wasSeen) {
      try {
        console.debug("[notif-ui] markSeen:request", { id: notification.id });
        const updated = await adminMarkNotificationSeen(notification.id);
        console.info("[notif-ui] markSeen:response", {
          id: notification.id,
          responseId: updated.id,
          seen: updated.seen,
        });
        await loadNotifications("after-mark-one");
      } catch (error) {
        console.error("[notif-ui] markSeen:error", {
          id: notification.id,
          error,
        });
        await loadNotifications("mark-one-error-resync");
      }
    }

    setNotifOpen(false);

    const targetPath = notification.targetPath || "/dashboard";
    const params = new URLSearchParams();
    if (notification.targetEntityId != null) {
      params.set("focusId", String(notification.targetEntityId));
    }
    if (notification.targetReference) {
      params.set("focusRef", notification.targetReference);
    }
    if (notification.targetEntityType) {
      params.set("focusType", notification.targetEntityType);
    }

    const query = params.toString();
    navigate(query ? `${targetPath}?${query}` : targetPath);
  }

  async function handleMarkAllSeen() {
    setNotifBusy(true);
    console.info("[notif-ui] markAllSeen:click", {
      unreadCountBefore: unreadCount,
      ids: notifications.map((n) => n.id),
    });

    try {
      console.debug("[notif-ui] markAllSeen:request");
      const result = await adminMarkAllNotificationsSeen();
      console.info("[notif-ui] markAllSeen:response", result);
      await loadNotifications("after-mark-all");
    } catch (error) {
      console.error("[notif-ui] markAllSeen:error", { error });
      await loadNotifications("mark-all-error-resync");
    } finally {
      setNotifBusy(false);
    }
  }

  function formatNotifTime(value?: string) {
    if (!value) return "";
    const d = new Date(value);
    if (Number.isNaN(d.getTime())) return "";
    return d.toLocaleString("fr-FR");
  }

  const fullNameLabel =
    [user?.firstname, user?.lastname].filter(Boolean).join(" ").trim() ||
    (user?.username ?? "Compte").trim() ||
    "Compte";
  const roleLabel = user?.role ? user.role.toUpperCase() : "UTILISATEUR";

  return (
    <header className="topbar">
      <div className="topLeft"></div>

      <div className="searchWrap">
        <input
          className="searchInput"
          placeholder="Rechercher…"
          aria-label="Rechercher"
          value={searchTerm}
          onChange={(e) => handleSearch(e.target.value)}
        />
        {searchResults.length > 0 && (
          <div className="searchDropdown">
            {searchResults.map((res, idx) => (
              <button
                key={idx}
                className="searchResultItem"
                onClick={() => handleResultClick(res.to)}
              >
                {res.label}
              </button>
            ))}
          </div>
        )}
      </div>

      <div className="topRight" style={{ position: "relative" }}>
        {isAdmin && (
          <div className="notifWrap">
            <button
              className="notifBtn"
              type="button"
              onClick={() => setNotifOpen((v) => !v)}
              aria-label="Notifications"
              title="Notifications"
            >
              <img
                src={notificationIcon}
                alt=""
                aria-hidden
                className="notifIcon"
              />
              {unreadCount > 0 && (
                <span className="notifBadge">
                  {unreadCount > 99 ? "99+" : unreadCount}
                </span>
              )}
            </button>

            {notifOpen && (
              <div className="notifPanel">
                <div className="notifPanelHeader">
                  <strong>Notifications</strong>
                  <button
                    className="topbarDropItem"
                    style={{ width: "auto", padding: "6px 10px" }}
                    type="button"
                    onClick={handleMarkAllSeen}
                    disabled={notifBusy}
                  >
                    Tout marquer lu
                  </button>
                </div>

                <div className="notifList">
                  {visibleNotifications.length === 0 ? (
                    <div className="notifEmpty">Aucune notification.</div>
                  ) : (
                    visibleNotifications.map((notification) => (
                      <button
                        key={notification.id}
                        className={`notifItem ${notification.seen ? "" : "notifItem--unread"}`}
                        onClick={() => handleNotificationClick(notification)}
                        type="button"
                      >
                        <div className="notifMessage">
                          {notification.message}
                        </div>
                        <div
                          className="notifMeta"
                          style={{ opacity: 0.8, marginTop: 4 }}
                        >
                          {formatNotifTime(notification.createdAt)} —{" "}
                          {notification.actor || "système"}
                        </div>
                      </button>
                    ))
                  )}
                </div>
              </div>
            )}
          </div>
        )}

        <div
          className="profileStatic"
          aria-label="Profil"
          title="Profil"
        >
          <span className="profileLogoWrap" aria-hidden>
            <img src={xLogo} alt="" className="profileLogo" />
          </span>
          <span className="profileText">
            <span className="profileName">{fullNameLabel}</span>
            <span className="profileRole">{roleLabel}</span>
          </span>
        </div>
      </div>
    </header>
  );
}
