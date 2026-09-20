export type Role = "admin" | "logistic" | "psf" | "pf";

export type MenuItem =
  | {
      type: "link";
      label: string;
      to: string;
      icon:
        | "dashboard"
        | "users"
        | "employees"
        | "nomenclature"
        | "stock"
        | "simulation"
        | "dept1"
        | "dept2"
        | "export"
        | "incoming"
        | "outgoing"
        | "production"
        | "logout";
    }
  | {
      type: "group";
      label: string;
      icon: "dept1";
      children: Array<{ label: string; to: string }>;
    };

export const MENU_BY_ROLE: Record<Role, MenuItem[]> = {
  admin: [
    {
      type: "link",
      label: "Tableau de bord",
      to: "/dashboard",
      icon: "dashboard",
    },
    { type: "link", label: "Produits", to: "/products", icon: "nomenclature" },
    { type: "link", label: "Utilisateurs", to: "/users", icon: "users" },
    { type: "link", label: "Employes", to: "/employees", icon: "employees" },
    {
      type: "link",
      label: "Nomenclature",
      to: "/nomenclature",
      icon: "nomenclature",
    },
    { type: "link", label: "Stock", to: "/stock", icon: "stock" },
    {
      type: "link",
      label: "Simulation",
      to: "/simulation",
      icon: "simulation",
    },
    {
      type: "link",
      label: "Retard de Production",
      to: "/prediction-retard-production",
      icon: "simulation",
    },

    {
      type: "group",
      label: "Département 1",
      icon: "dept1",
      children: [
        { label: "Stock département 1", to: "/departement-1/stock" },
        { label: "Production", to: "/departement-1/production" },
        { label: "Entrées", to: "/departement-1/incoming" },
        { label: "Sorties", to: "/psf/outgoing" },
      ],
    },

    {
      type: "link",
      label: "Département 2",
      to: "/departement-2",
      icon: "dept2",
    },
    { type: "link", label: "Export", to: "/export", icon: "export" },
    { type: "link", label: "Déconnexion", to: "/logout", icon: "logout" },
  ],

  logistic: [
    { type: "link", label: "Entrées", to: "/incoming", icon: "incoming" },
    {
      type: "link",
      label: "Sorties",
      to: "/logistic/outgoing",
      icon: "outgoing",
    },
    {
      type: "link",
      label: "Stock département 1",
      to: "/departement-1/stock",
      icon: "stock",
    },
    { type: "link", label: "Déconnexion", to: "/logout", icon: "logout" },
  ],

  psf: [
    {
      type: "link",
      label: "Production",
      to: "/departement-1/production",
      icon: "production",
    },
    { type: "link", label: "Sorties", to: "/psf/outgoing", icon: "outgoing" },
    { type: "link", label: "Déconnexion", to: "/logout", icon: "logout" },
  ],

  pf: [
    {
      type: "link",
      label: "Nouvelle Production",
      to: "/production",
      icon: "production",
    },
    {
      type: "link",
      label: "Production Département 2",
      to: "/departement-2",
      icon: "dept2",
    },
    { type: "link", label: "Déconnexion", to: "/logout", icon: "logout" },
  ],
};
