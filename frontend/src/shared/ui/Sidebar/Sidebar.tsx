import { useEffect, useMemo, useRef, useState } from "react";
import { createPortal } from "react-dom";
import { NavLink } from "react-router-dom";
import "./sidebar.css";
import type { Role } from "./menu";
import { MENU_BY_ROLE } from "./menu";
import { useAuth } from "../../../app/providers/AuthProvider";
import logoImg from "../../../assets/X_white.png";

type SidebarProps = {
    role: Role;
    collapsedWidth?: number;
    expandedWidth?: number;
    onWidthChange?: (width: number) => void;
};

function cx(...parts: Array<string | false | null | undefined>) {
    return parts.filter(Boolean).join(" ");
}

function Icon({ name }: { name: string }) {
    const common = { className: "sbIcon", viewBox: "0 0 24 24" as const, fill: "none" as const };
    switch (name) {
        case "dashboard":
            return (
                <svg {...common}>
                    <path d="M4 13h7V4H4v9Zm9 7h7V11h-7v9ZM4 20h7v-5H4v5Zm9-9h7V4h-7v7Z" stroke="currentColor" strokeWidth="1.7" />
                </svg>
            );
        case "users":
            return (
                <svg {...common}>
                    <path d="M16 11c1.66 0 3-1.57 3-3.5S17.66 4 16 4s-3 1.57-3 3.5S14.34 11 16 11Z" stroke="currentColor" strokeWidth="1.7" />
                    <path d="M8 11c1.66 0 3-1.57 3-3.5S9.66 4 8 4 5 5.57 5 7.5 6.34 11 8 11Z" stroke="currentColor" strokeWidth="1.7" />
                    <path d="M3.5 20c.6-3 3-5 6-5" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" />
                    <path d="M20.5 20c-.6-3-3-5-6-5" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" />
                </svg>
            );
        case "employees":
            return (
                <svg {...common}>
                    <path d="M12 11c2.21 0 4-1.79 4-4S14.21 3 12 3 8 4.79 8 7s1.79 4 4 4Z" stroke="currentColor" strokeWidth="1.7" />
                    <path d="M5 21c.7-3.2 3.3-5 7-5s6.3 1.8 7 5" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" />
                </svg>
            );
        case "nomenclature":
            return (
                <svg {...common}>
                    <path d="M7 7h10M7 12h10M7 17h10" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" />
                </svg>
            );
        case "stock":
            return (
                <svg {...common}>
                    <path d="M4 7l8-4 8 4-8 4-8-4Z" stroke="currentColor" strokeWidth="1.7" />
                    <path d="M4 7v10l8 4 8-4V7" stroke="currentColor" strokeWidth="1.7" />
                    <path d="M12 11v10" stroke="currentColor" strokeWidth="1.7" />
                </svg>
            );
        case "simulation":
            return (
                <svg {...common}>
                    <path d="M4 18V6m4 12V9m4 9V4m4 14v-8m4 8V11" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" />
                </svg>
            );
        case "dept1":
        case "dept2":
            return (
                <svg {...common}>
                    <path d="M4 20V7l8-4 8 4v13" stroke="currentColor" strokeWidth="1.7" />
                    <path d="M9 20v-6h6v6" stroke="currentColor" strokeWidth="1.7" />
                </svg>
            );
        case "export":
            return (
                <svg {...common}>
                    <path d="M12 3v10" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" />
                    <path d="M8 7l4-4 4 4" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" strokeLinejoin="round" />
                    <path d="M5 14v6h14v-6" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" />
                </svg>
            );
        case "incoming":
            return (
                <svg {...common}>
                    <path d="M12 20V10" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" />
                    <path d="M8 14l4 4 4-4" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" strokeLinejoin="round" />
                    <path d="M5 4h14" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" />
                </svg>
            );
        case "outgoing":
            return (
                <svg {...common}>
                    <path d="M12 4v10" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" />
                    <path d="M8 10l4-4 4 4" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" strokeLinejoin="round" />
                    <path d="M5 20h14" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" />
                </svg>
            );
        case "production":
            return (
                <svg {...common}>
                    <path d="M7 20V10l5-3 5 3v10" stroke="currentColor" strokeWidth="1.7" />
                    <path d="M9.5 12h5" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" />
                </svg>
            );
        case "logout":
            return (
                <svg {...common}>
                    <path d="M9 21H5a2 2 0 01-2-2V5a2 2 0 012-2h4" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" />
                    <path d="M16 17l5-5-5-5" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" strokeLinejoin="round" />
                    <path d="M21 12H9" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" />
                </svg>
            );
        default:
            return (
                <svg {...common}>
                    <circle cx="12" cy="12" r="9" stroke="currentColor" strokeWidth="1.7" />
                </svg>
            );
    }
}

export default function Sidebar({
    role,
    collapsedWidth = 96,
    expandedWidth = 260,
    onWidthChange,
}: SidebarProps) {
    const { logout } = useAuth();
    const menu = useMemo(() => MENU_BY_ROLE[role as Role], [role]);

    const [collapsed, setCollapsed] = useState<boolean>(() => localStorage.getItem("sb_collapsed") === "1");
    const [dept1Open, setDept1Open] = useState<boolean>(false);

    const asideRef = useRef<HTMLElement | null>(null);

    const [flyout, setFlyout] = useState<{
        open: boolean;
        top: number;
        left: number;
        items: Array<{ label: string; to: string }>;
    }>({ open: false, top: 0, left: 0, items: [] });

    const closeTimer = useRef<number | null>(null);

    const widthPx = collapsed ? collapsedWidth : expandedWidth;

    useEffect(() => {
        localStorage.setItem("sb_collapsed", collapsed ? "1" : "0");
        onWidthChange?.(widthPx);
    }, [collapsed, widthPx, onWidthChange]);

    useEffect(() => {
        onWidthChange?.(widthPx);
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, []);

    function clearCloseTimer() {
        if (closeTimer.current) {
            window.clearTimeout(closeTimer.current);
            closeTimer.current = null;
        }
    }

    function scheduleCloseFlyout() {
        clearCloseTimer();
        closeTimer.current = window.setTimeout(() => {
            setFlyout((f) => ({ ...f, open: false }));
        }, 320);
    }

    function openFlyoutFrom(el: HTMLElement, items: Array<{ label: string; to: string }>) {
        clearCloseTimer();
        const r = el.getBoundingClientRect();

        setFlyout({
            open: true,
            top: r.top,
            left: widthPx + 12,
            items,
        });
    }

    function closeFlyoutNow() {
        clearCloseTimer();
        setFlyout((f) => ({ ...f, open: false }));
    }

    return (
        <aside
            ref={(node) => (asideRef.current = node)}
            className={cx("sb", collapsed && "sb--collapsed")}
            style={{ width: widthPx }}
        >
            <div className="sbHeader">
                <div className="sbLogoBox" title={collapsed ? "ERP" : undefined}>
                    <img src={logoImg} alt="Logo" className="sbLogoImg" />
                </div>

                <span className={cx("sbLogoText", collapsed && "sbLogoText--hidden")}>Système ERP</span>

                <button
                    className="sbToggle"
                    type="button"
                    onClick={() => {
                        setCollapsed((v) => !v);
                        closeFlyoutNow();
                    }}
                    aria-label={collapsed ? "Déployer" : "Réduire"}
                    title={collapsed ? "Déployer" : "Réduire"}
                >
                    <svg className={cx("sbChevron", collapsed && "sbChevron--collapsed")} viewBox="0 0 24 24">
                        <path d="M15 6l-6 6 6 6" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
                    </svg>
                </button>
            </div>

            <div className="sbDivider" />

            <nav className="sbNav" onMouseLeave={() => collapsed && scheduleCloseFlyout()}>
                {menu.filter((i) => i.type === "group" || i.to !== "/logout").map((item, idx) => {
                    if (item.type === "link") {
                        return (
                            <NavLink
                                key={`${item.to}-${idx}`}
                                to={item.to}
                                className={({ isActive }) => cx("sbItem", isActive && "sbItem--active")}
                                title={collapsed ? item.label : undefined}
                            >
                                <Icon name={item.icon} />
                                <span className="sbLabel">{item.label}</span>
                            </NavLink>
                        );
                    }

                    const isOpen = dept1Open;

                    return (
                        <div key={`group-${idx}`} className="sbGroup">
                            <button
                                type="button"
                                className={cx("sbItem", "sbItem--button")}
                                onClick={() => {
                                    if (!collapsed) setDept1Open((v) => !v);
                                }}
                                onMouseEnter={(e) => {
                                    if (collapsed) openFlyoutFrom(e.currentTarget, item.children);
                                }}
                                onMouseLeave={() => {
                                    if (collapsed) scheduleCloseFlyout();
                                }}
                                onFocus={(e) => {
                                    if (collapsed) openFlyoutFrom(e.currentTarget, item.children);
                                }}
                                onBlur={() => {
                                    if (collapsed) scheduleCloseFlyout();
                                }}
                                title={collapsed ? item.label : undefined}
                            >
                                <Icon name={item.icon} />
                                <span className="sbLabel">{item.label}</span>
                                {!collapsed && (
                                    <span className={cx("sbCaret", isOpen && "sbCaret--open")} aria-hidden="true">
                                        ▾
                                    </span>
                                )}
                            </button>

                            {!collapsed && (
                                <div className={cx("sbSub", isOpen && "sbSub--open")}>
                                    {item.children.map((ch) => (
                                        <NavLink
                                            key={ch.to}
                                            to={ch.to}
                                            className={({ isActive }) => cx("sbSubItem", isActive && "sbSubItem--active")}
                                        >
                                            <span className="sbBullet" aria-hidden="true" />
                                            <span className="sbSubLabel">{ch.label}</span>
                                        </NavLink>
                                    ))}
                                </div>
                            )}
                        </div>
                    );
                })}

                {collapsed && flyout.open && createPortal(
                    <div
                        className="sbFlyout"
                        style={{ top: flyout.top, left: flyout.left }}
                        onMouseEnter={() => {
                            clearCloseTimer();
                            setFlyout((f) => ({ ...f, open: true }));
                        }}
                        onMouseLeave={scheduleCloseFlyout}
                    >
                        {flyout.items.map((ch) => (
                            <NavLink
                                key={ch.to}
                                to={ch.to}
                                className={({ isActive }) => cx("sbFlyItem", isActive && "sbFlyItem--active")}
                                onClick={closeFlyoutNow}
                            >
                                {ch.label}
                            </NavLink>
                        ))}
                    </div>,
                    document.body
                )}
            </nav>

            <div className="sbStickyBottom">
                <button
                    type="button"
                    className="sbItem sbItem--button sbItem--logout"
                    onClick={logout}
                    title={collapsed ? "Déconnexion" : undefined}
                >
                    <Icon name="logout" />
                    <span className="sbLabel">Déconnexion</span>
                </button>

                <div className="sbFooter">
                    <div className="sbFooterHint">{collapsed ? "" : "© Axone"}</div>
                </div>
            </div>
        </aside>
    );
}
