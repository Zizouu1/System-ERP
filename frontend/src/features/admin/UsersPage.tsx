import { useEffect, useMemo, useState } from "react";
import editIcon from "@/assets/edit.png";
import { Card } from "@/components/Card";
import { DataTable } from "@/components/DataTable";
import { Modal } from "@/components/Modal";
import { Spinner } from "@/components/Spinner";
import {
  adminCreateUser,
  adminDeleteUser,
  adminListUsers,
  adminUpdateUser,
} from "@/shared/api/endpoints";
import type { ApiRole, UserDto } from "@/shared/api/types";
import { TableToolbar } from "@/shared/ui/TableToolbar/TableToolbar";
import { useConfirmationDialog } from "@/shared/ui/ConfirmationDialog/useConfirmationDialog";
import { matchesExact, matchesText } from "@/utils/tableFilters";

const ROLES: ApiRole[] = ["ADMIN", "LOGISTIC", "PSF", "PF"];
type UserTableFilters = {
  username: string;
  fullName: string;
  matricule: string;
  role: string;
};
const EMPTY_FILTERS: UserTableFilters = {
  username: "",
  fullName: "",
  matricule: "",
  role: "",
};

export default function UsersPage() {
  const { askConfirmation, confirmationDialog } = useConfirmationDialog();
  const [loading, setLoading] = useState(true);
  const [users, setUsers] = useState<UserDto[]>([]);
  const [filters, setFilters] = useState<UserTableFilters>(EMPTY_FILTERS);
  const [draftFilters, setDraftFilters] =
    useState<UserTableFilters>(EMPTY_FILTERS);
  const [selectedIds, setSelectedIds] = useState<(string | number)[]>([]);

  // create
  const [createOpen, setCreateOpen] = useState(false);
  const [cUsername, setCUsername] = useState("");
  const [cFirstname, setCFirstname] = useState("");
  const [cLastname, setCLastname] = useState("");
  const [cMatricule, setCMatricule] = useState("");
  const [cPassword, setCPassword] = useState("");
  const [cRole, setCRole] = useState<ApiRole>("LOGISTIC");
  const [msg, setMsg] = useState<string | null>(null);

  // edit
  const [editOpen, setEditOpen] = useState(false);
  const [editUser, setEditUser] = useState<UserDto | null>(null);
  const [eUsername, setEUsername] = useState("");
  const [eFirstname, setEFirstname] = useState("");
  const [eLastname, setELastname] = useState("");
  const [eMatricule, setEMatricule] = useState("");
  const [eRole, setERole] = useState<ApiRole>("LOGISTIC");
  const [ePassword, setEPassword] = useState("");

  async function refresh() {
    setLoading(true);
    try {
      const data = await adminListUsers();
      setUsers(data);
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    refresh();
  }, []);

  const filtered = useMemo(() => {
    return users.filter((u) => {
      const fullName = `${u.firstname ?? ""} ${u.lastname ?? ""}`.trim();
      return (
        matchesText(u.username, filters.username) &&
        matchesText(fullName, filters.fullName) &&
        matchesText(u.matricule ?? "", filters.matricule) &&
        matchesExact(u.role, filters.role)
      );
    });
  }, [users, filters]);

  async function create(e: React.FormEvent) {
    e.preventDefault();
    setMsg(null);
    try {
      await adminCreateUser({
        username: cUsername.trim(),
        password: cPassword,
        role: cRole,
        firstname: cFirstname.trim() || undefined,
        lastname: cLastname.trim() || undefined,
        matricule: cMatricule.trim() || undefined,
      });
      setCUsername("");
      setCPassword("");
      setCFirstname("");
      setCLastname("");
      setCMatricule("");
      setCRole("LOGISTIC");
      setCreateOpen(false);
      await refresh();
    } catch (e: any) {
      setMsg(e?.message ?? "Échec de l'opération.");
    }
  }

  async function openEdit(u: UserDto) {
    setEditUser(u);
    setEFirstname(u.firstname ?? "");
    setELastname(u.lastname ?? "");
    setEMatricule(u.matricule ?? "");
    setEUsername(u.username);
    setERole(u.role as ApiRole);
    setEPassword("");
    setEditOpen(true);
  }

  async function saveEdit() {
    if (!editUser) return;
    setMsg(null);
    try {
      await adminUpdateUser(editUser.id, {
        username: eUsername.trim(),
        firstname: eFirstname.trim() || undefined,
        lastname: eLastname.trim() || undefined,
        matricule: eMatricule.trim() || undefined,
        role: eRole,
        password: ePassword.trim() || undefined,
      });
      setEditOpen(false);
      setEditUser(null);
      await refresh();
    } catch (e: any) {
      setMsg(e?.message ?? "Échec.");
    }
  }

  async function remove(id: number) {
    const confirmed = await askConfirmation({
      title: "Supprimer un utilisateur",
      message: "Voulez-vous vraiment supprimer cet utilisateur ?",
    });
    if (!confirmed) return;
    setMsg(null);
    try {
      await adminDeleteUser(id);
      setMsg("Supprimé avec succès.");
      await refresh();
    } catch (e: any) {
      setMsg(e?.message ?? "Échec.");
    }
  }

  async function handleGlobalDelete() {
    const confirmed = await askConfirmation({
      title: "Supprimer",
      message: `Voulez-vous vraiment supprimer ${selectedIds.length} utilisateur(s) ?`,
    });
    if (!confirmed) return;
    setMsg(null);
    try {
      for (const id of selectedIds) {
        await adminDeleteUser(Number(id));
      }
      setMsg("Supprimés avec succès.");
      setSelectedIds([]);
      await refresh();
    } catch (e: any) {
      setMsg(e?.message ?? "Échec lors de la suppression.");
    }
  }

  return (
    <div className="page">
      <div className="grid">
        <Card title="Utilisateurs">
          <TableToolbar
            exportFilename="utilisateurs"
            exportTitle="Liste des utilisateurs"
            rows={
              selectedIds.length > 0
                ? filtered.filter((u) => selectedIds.includes(u.id))
                : filtered
            }
            selectedCount={selectedIds.length}
            onRefresh={refresh}
            onGlobalDelete={handleGlobalDelete}
            columns={[
              { header: "Utilisateur", cell: (u) => u.username },
              {
                header: "Nom",
                cell: (u) =>
                  `${u.firstname ?? ""} ${u.lastname ?? ""}`.trim() || "-",
              },
              { header: "Matricule", cell: (u) => u.matricule ?? "-" },
              {
                header: "Rôle",
                cell: (u) => (u.role === "LOGISTIC" ? "LOGISTIQUE" : u.role),
              },
            ]}
            filterContent={
              <>
                <div className="field">
                  <div className="label">Utilisateur</div>
                  <input
                    className="input"
                    value={draftFilters.username}
                    onChange={(e) =>
                      setDraftFilters((prev) => ({
                        ...prev,
                        username: e.target.value,
                      }))
                    }
                    placeholder="Ex: admin"
                  />
                </div>
                <div className="field">
                  <div className="label">Nom / Prénom</div>
                  <input
                    className="input"
                    value={draftFilters.fullName}
                    onChange={(e) =>
                      setDraftFilters((prev) => ({
                        ...prev,
                        fullName: e.target.value,
                      }))
                    }
                    placeholder="Ex: Ben Ali"
                  />
                </div>
                <div className="field">
                  <div className="label">Matricule</div>
                  <input
                    className="input"
                    value={draftFilters.matricule}
                    onChange={(e) =>
                      setDraftFilters((prev) => ({
                        ...prev,
                        matricule: e.target.value,
                      }))
                    }
                    placeholder="Ex: MAT-001"
                  />
                </div>
                <div className="field">
                  <div className="label">Rôle</div>
                  <select
                    className="select"
                    value={draftFilters.role}
                    onChange={(e) =>
                      setDraftFilters((prev) => ({
                        ...prev,
                        role: e.target.value,
                      }))
                    }
                  >
                    <option value="">Tous</option>
                    {ROLES.map((r) => (
                      <option key={r} value={r}>
                        {r === "LOGISTIC" ? "LOGISTIQUE" : r}
                      </option>
                    ))}
                  </select>
                </div>
              </>
            }
            onApplyFilters={() => setFilters({ ...draftFilters })}
            extraActions={
              <button
                className="btn btnPrimary"
                onClick={() => {
                  setMsg(null);
                  setCreateOpen(true);
                }}
              >
                Créer utilisateur
              </button>
            }
          />
          {loading ? (
            <Spinner />
          ) : (
            <DataTable
              enableSelection
              selectedKeys={selectedIds}
              onSelectionChange={setSelectedIds}
              rows={filtered}
              rowKey={(u) => u.id}
              columns={[
                { header: "Utilisateur", cell: (u) => u.username },
                {
                  header: "Nom",
                  cell: (u) =>
                    `${u.firstname ?? ""} ${u.lastname ?? ""}`.trim() || "-",
                },
                {
                  header: "Matricule",
                  cell: (u) => u.matricule ?? "-",
                },
                {
                  header: "Rôle",
                  cell: (u) => (
                    <span className="badge badgeBlue">
                      {u.role === "LOGISTIC" ? "LOGISTIQUE" : u.role}
                    </span>
                  ),
                },
                {
                  header: "",
                  cell: (u) => (
                    <div
                      className="actionsRow"
                      style={{ gap: 8, justifyContent: "flex-end" }}
                    >
                      <button
                        className="btn btnGhost"
                        style={{ padding: 4 }}
                        onClick={() => openEdit(u)}
                        title="Modifier"
                      >
                        <img
                          src={editIcon}
                          alt="Edit"
                          style={{ width: 16, height: 16 }}
                        />
                      </button>
                    </div>
                  ),
                  className: "col-actions",
                },
              ]}
            />
          )}
        </Card>
      </div>

      <Modal
        open={createOpen}
        title="Nouvel utilisateur"
        onClose={() => setCreateOpen(false)}
        actions={null}
      >
        <form className="form" onSubmit={create}>
          <div className="field">
            <div className="label">Nom d'utilisateur</div>
            <input
              className="input"
              value={cUsername}
              onChange={(e) => setCUsername(e.target.value)}
              required
            />
          </div>

          <div className="grid2">
            <div className="field">
              <div className="label">Prénom</div>
              <input
                className="input"
                value={cFirstname}
                onChange={(e) => setCFirstname(e.target.value)}
              />
            </div>
            <div className="field">
              <div className="label">Nom</div>
              <input
                className="input"
                value={cLastname}
                onChange={(e) => setCLastname(e.target.value)}
              />
            </div>
          </div>

          <div className="field">
            <div className="label">Matricule</div>
            <input
              className="input"
              value={cMatricule}
              onChange={(e) => setCMatricule(e.target.value)}
              placeholder="ex: MAT-001"
            />
          </div>

          <div className="grid2">
            <div className="field">
              <div className="label">Rôle</div>
              <select
                className="select"
                value={cRole}
                onChange={(e) => setCRole(e.target.value as ApiRole)}
              >
                {ROLES.map((r) => (
                  <option key={r} value={r}>
                    {r === "LOGISTIC" ? "LOGISTIQUE" : r}
                  </option>
                ))}
              </select>
            </div>

            <div className="field">
              <div className="label">Mot de passe</div>
              <input
                className="input"
                type="password"
                value={cPassword}
                onChange={(e) => setCPassword(e.target.value)}
                required
              />
            </div>
          </div>

          <button className="btn btnPrimary">Créer</button>

          {msg && <div className="notice">{msg}</div>}
        </form>
      </Modal>

      <Modal
        open={editOpen}
        title="Modifier l'utilisateur"
        onClose={() => setEditOpen(false)}
        actions={
          <button className="btn btnPrimary" onClick={saveEdit}>
            Enregistrer
          </button>
        }
      >
        <div className="form">
          <div className="field">
            <div className="label">Nom d'utilisateur</div>
            <input
              className="input"
              value={eUsername}
              onChange={(e) => setEUsername(e.target.value)}
            />
          </div>
          <div className="grid2">
            <div className="field">
              <div className="label">Prénom</div>
              <input
                className="input"
                value={eFirstname}
                onChange={(e) => setEFirstname(e.target.value)}
              />
            </div>
            <div className="field">
              <div className="label">Nom</div>
              <input
                className="input"
                value={eLastname}
                onChange={(e) => setELastname(e.target.value)}
              />
            </div>
          </div>

          <div className="field">
            <div className="label">Matricule</div>
            <input
              className="input"
              value={eMatricule}
              onChange={(e) => setEMatricule(e.target.value)}
              placeholder="ex: MAT-001"
            />
          </div>

          <div className="field">
            <div className="label">Rôle</div>
            <select
              className="select"
              value={eRole}
              onChange={(e) => setERole(e.target.value as ApiRole)}
            >
              {ROLES.map((r) => (
                <option key={r} value={r}>
                  {r === "LOGISTIC" ? "LOGISTIQUE" : r}
                </option>
              ))}
            </select>
          </div>

          <div className="field">
            <div className="label">Nouveau mot de passe</div>
            <input
              className="input"
              type="password"
              value={ePassword}
              onChange={(e) => setEPassword(e.target.value)}
              placeholder="Laisser vide pour conserver"
            />
          </div>
        </div>
      </Modal>
      {confirmationDialog}
    </div>
  );
}
