import { useEffect, useMemo, useState } from "react";
import editIcon from "@/assets/edit.png";
import { Card } from "@/components/Card";
import { DataTable } from "@/components/DataTable";
import { Modal } from "@/components/Modal";
import { Spinner } from "@/components/Spinner";
import {
  adminCreateEmployee,
  adminDeleteEmployee,
  adminListEmployees,
  adminUpdateEmployee,
  importEmployees,
} from "@/shared/api/endpoints";
import type { EmployeeDto, EmployeeUpsertReq } from "@/shared/api/types";
import { TableToolbar } from "@/shared/ui/TableToolbar/TableToolbar";
import { useConfirmationDialog } from "@/shared/ui/ConfirmationDialog/useConfirmationDialog";
import { matchesExact, matchesText } from "@/utils/tableFilters";

type EmployeeFormState = {
  matricule: string;
  nom: string;
  prenom: string;
  poste: string;
  departement: string;
};

const EMPTY_FORM: EmployeeFormState = {
  matricule: "",
  nom: "",
  prenom: "",
  poste: "",
  departement: "",
};
type EmployeeTableFilters = {
  matricule: string;
  nomPrenom: string;
  poste: string;
  departement: string;
};
const EMPTY_FILTERS: EmployeeTableFilters = {
  matricule: "",
  nomPrenom: "",
  poste: "",
  departement: "",
};

const MATRICULE_MAX_LENGTH = 50;
const FIELD_MAX_LENGTH = 120;

function normalizeValue(value: string) {
  return value.trim();
}

function validateForm(form: EmployeeFormState): string | null {
  const requiredFields: Array<keyof EmployeeFormState> = [
    "matricule",
    "nom",
    "prenom",
    "poste",
    "departement",
  ];

  for (const field of requiredFields) {
    if (!normalizeValue(form[field])) {
      return "Tous les champs sont obligatoires.";
    }
  }

  if (normalizeValue(form.matricule).length > MATRICULE_MAX_LENGTH) {
    return `Le matricule ne peut pas depasser ${MATRICULE_MAX_LENGTH} caracteres.`;
  }

  const longField = (["nom", "prenom", "poste", "departement"] as const).find(
    (field) => normalizeValue(form[field]).length > FIELD_MAX_LENGTH,
  );
  if (longField) {
    return `Le champ '${longField}' ne peut pas depasser ${FIELD_MAX_LENGTH} caracteres.`;
  }

  return null;
}

function toRequest(form: EmployeeFormState): EmployeeUpsertReq {
  return {
    matricule: normalizeValue(form.matricule),
    nom: normalizeValue(form.nom),
    prenom: normalizeValue(form.prenom),
    poste: normalizeValue(form.poste),
    departement: normalizeValue(form.departement),
  };
}

export default function EmployeesPage() {
  const { askConfirmation, confirmationDialog } = useConfirmationDialog();
  const [loading, setLoading] = useState(true);
  const [rows, setRows] = useState<EmployeeDto[]>([]);
  const [filters, setFilters] = useState<EmployeeTableFilters>(EMPTY_FILTERS);
  const [draftFilters, setDraftFilters] =
    useState<EmployeeTableFilters>(EMPTY_FILTERS);
  const [selectedIds, setSelectedIds] = useState<(string | number)[]>([]);
  const [msg, setMsg] = useState<string | null>(null);

  const [createOpen, setCreateOpen] = useState(false);
  const [createForm, setCreateForm] = useState<EmployeeFormState>(EMPTY_FORM);

  const [editOpen, setEditOpen] = useState(false);
  const [editRow, setEditRow] = useState<EmployeeDto | null>(null);
  const [editForm, setEditForm] = useState<EmployeeFormState>(EMPTY_FORM);

  async function refresh() {
    setLoading(true);
    try {
      setRows(await adminListEmployees());
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    refresh();
  }, []);

  const departementOptions = useMemo(
    () =>
      Array.from(new Set(rows.map((row) => row.departement)))
        .filter(Boolean)
        .sort((a, b) => a.localeCompare(b)),
    [rows],
  );

  const posteOptions = useMemo(
    () =>
      Array.from(new Set(rows.map((row) => row.poste)))
        .filter(Boolean)
        .sort((a, b) => a.localeCompare(b)),
    [rows],
  );

  const filtered = useMemo(() => {
    return rows.filter((row) => {
      const fullName = `${row.nom} ${row.prenom}`.trim();
      return (
        matchesText(row.matricule, filters.matricule) &&
        matchesText(fullName, filters.nomPrenom) &&
        matchesExact(row.departement, filters.departement) &&
        matchesExact(row.poste, filters.poste)
      );
    });
  }, [rows, filters]);

  function openCreateModal() {
    setCreateForm(EMPTY_FORM);
    setMsg(null);
    setCreateOpen(true);
  }

  function openEditModal(row: EmployeeDto) {
    setEditRow(row);
    setEditForm({
      matricule: row.matricule,
      nom: row.nom,
      prenom: row.prenom,
      poste: row.poste,
      departement: row.departement,
    });
    setMsg(null);
    setEditOpen(true);
  }

  async function createEmployee(event: React.FormEvent) {
    event.preventDefault();

    const validationError = validateForm(createForm);
    if (validationError) {
      setMsg(validationError);
      return;
    }

    try {
      await adminCreateEmployee(toRequest(createForm));
      setCreateOpen(false);
      await refresh();
    } catch (error: any) {
      setMsg(error?.message ?? "Erreur lors de l'ajout.");
    }
  }

  async function saveEdit() {
    if (!editRow) return;

    const validationError = validateForm(editForm);
    if (validationError) {
      setMsg(validationError);
      return;
    }

    try {
      await adminUpdateEmployee(editRow.id, toRequest(editForm));
      setEditOpen(false);
      setEditRow(null);
      await refresh();
    } catch (error: any) {
      setMsg(error?.message ?? "Erreur lors de la mise a jour.");
    }
  }

  async function removeEmployee(id: number) {
    const confirmed = await askConfirmation({
      title: "Supprimer un employe",
      message: "Voulez-vous vraiment supprimer cet employe ?",
    });
    if (!confirmed) return;

    try {
      await adminDeleteEmployee(id);
      setMsg("Employe supprime avec succes.");
      await refresh();
    } catch (error: any) {
      setMsg(error?.message ?? "Erreur lors de la suppression.");
    }
  }

  async function handleGlobalDelete() {
    const confirmed = await askConfirmation({
      title: "Supprimer",
      message: `Voulez-vous vraiment supprimer ${selectedIds.length} employé(s) ?`,
    });
    if (!confirmed) return;
    setMsg(null);
    setLoading(true);
    try {
      for (const id of selectedIds) {
        await adminDeleteEmployee(Number(id));
      }
      setMsg("Supprimés avec succès.");
      setSelectedIds([]);
      await refresh();
    } catch (error: any) {
      setMsg(error?.message ?? "Erreur lors de la suppression.");
    } finally {
      setLoading(false);
    }
  }

  async function handleCsv(event: React.ChangeEvent<HTMLInputElement>) {
    const file = event.target.files?.[0];
    if (!file) return;

    setLoading(true);
    setMsg(null);

    try {
      const text = await file.text();
      const lines = text.split(/\r?\n/).filter((line) => line.trim());
      if (lines.length < 2) {
        throw new Error("Fichier vide ou mal formate.");
      }

      const separator = lines[0].includes(";") ? ";" : ",";
      const headers = lines[0].split(separator).map((header) =>
        header
          .trim()
          .toLowerCase()
          .normalize("NFD")
          .replace(/[\u0300-\u036f]/g, ""),
      );
      const dataRows = lines
        .slice(1)
        .map((line) => line.split(separator).map((cell) => cell.trim()));

      const idxMatricule = headers.findIndex((h) =>
        ["matricule", "code", "id"].includes(h),
      );
      const idxNom = headers.findIndex((h) => ["nom", "lastname"].includes(h));
      const idxPrenom = headers.findIndex((h) =>
        ["prenom", "firstname"].includes(h),
      );
      const idxPoste = headers.findIndex((h) =>
        ["poste", "fonction"].includes(h),
      );
      const idxDepartement = headers.findIndex((h) =>
        ["departement", "department", "service"].includes(h),
      );

      const rowsForImport =
        idxMatricule !== -1 &&
        idxNom !== -1 &&
        idxPrenom !== -1 &&
        idxPoste !== -1 &&
        idxDepartement !== -1
          ? dataRows.map((row) => [
              row[idxMatricule] || "",
              row[idxNom] || "",
              row[idxPrenom] || "",
              row[idxPoste] || "",
              row[idxDepartement] || "",
            ])
          : dataRows.map((row) => [
              row[0] || "",
              row[1] || "",
              row[2] || "",
              row[3] || "",
              row[4] || "",
            ]);

      await importEmployees(rowsForImport);
      await refresh();
    } catch (error: any) {
      setMsg(error?.message ?? "Erreur import.");
    } finally {
      setLoading(false);
      if (event.target) {
        event.target.value = "";
      }
    }
  }

  return (
    <div className="page">
      <div className="grid">
        <Card title="Employes">
          <TableToolbar
            exportFilename="employes"
            exportTitle="Liste des employes"
            rows={
              selectedIds.length > 0
                ? filtered.filter((p) => selectedIds.includes(p.id))
                : filtered
            }
            selectedCount={selectedIds.length}
            onRefresh={refresh}
            onGlobalDelete={handleGlobalDelete}
            columns={[
              { header: "Matricule", cell: (row) => row.matricule },
              { header: "Nom", cell: (row) => row.nom },
              { header: "Prenom", cell: (row) => row.prenom },
              { header: "Poste", cell: (row) => row.poste },
              { header: "Departement", cell: (row) => row.departement },
            ]}
            onImport={() => {
              const input = document.createElement("input");
              input.type = "file";
              input.accept = ".csv";
              input.onchange = (event) => handleCsv(event as any);
              input.click();
            }}
            importLabel="Importer CSV"
            filterContent={
              <>
                <div className="field">
                  <div className="label">Matricule</div>
                  <input
                    className="input"
                    value={draftFilters.matricule}
                    onChange={(event) =>
                      setDraftFilters((prev) => ({
                        ...prev,
                        matricule: event.target.value,
                      }))
                    }
                    placeholder="Ex: MAT-001"
                  />
                </div>
                <div className="field">
                  <div className="label">Nom / Prénom</div>
                  <input
                    className="input"
                    value={draftFilters.nomPrenom}
                    onChange={(event) =>
                      setDraftFilters((prev) => ({
                        ...prev,
                        nomPrenom: event.target.value,
                      }))
                    }
                    placeholder="Ex: Ben Ali"
                  />
                </div>
                <div className="field">
                  <div className="label">Departement</div>
                  <select
                    className="select"
                    value={draftFilters.departement}
                    onChange={(event) =>
                      setDraftFilters((prev) => ({
                        ...prev,
                        departement: event.target.value,
                      }))
                    }
                  >
                    <option value="">Tous les departements</option>
                    {departementOptions.map((departement) => (
                      <option key={departement} value={departement}>
                        {departement}
                      </option>
                    ))}
                  </select>
                </div>
                <div className="field">
                  <div className="label">Poste</div>
                  <select
                    className="select"
                    value={draftFilters.poste}
                    onChange={(event) =>
                      setDraftFilters((prev) => ({
                        ...prev,
                        poste: event.target.value,
                      }))
                    }
                  >
                    <option value="">Tous les postes</option>
                    {posteOptions.map((poste) => (
                      <option key={poste} value={poste}>
                        {poste}
                      </option>
                    ))}
                  </select>
                </div>
              </>
            }
            onApplyFilters={() => setFilters({ ...draftFilters })}
            extraActions={
              <button className="btn btnPrimary" onClick={openCreateModal}>
                Ajouter
              </button>
            }
          />

          {msg && (
            <div className="notice" style={{ marginBottom: 12 }}>
              {msg}
            </div>
          )}

          {loading ? (
            <Spinner />
          ) : (
            <DataTable
              enableSelection
              selectedKeys={selectedIds}
              onSelectionChange={setSelectedIds}
              rows={filtered}
              rowKey={(row) => row.id}
              columns={[
                { header: "Matricule", cell: (row) => row.matricule },
                { header: "Nom", cell: (row) => row.nom },
                { header: "Prenom", cell: (row) => row.prenom },
                {
                  header: "Poste",
                  cell: (row) => (
                    <span className="badge badgeBlue">{row.poste}</span>
                  ),
                },
                { header: "Departement", cell: (row) => row.departement },
                {
                  header: "",
                  cell: (row) => (
                    <div
                      className="actionsRow"
                      style={{ justifyContent: "flex-end" }}
                    >
                      <button
                        className="btn btnGhost"
                        style={{ padding: 4 }}
                        onClick={() => openEditModal(row)}
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
                },
              ]}
            />
          )}
        </Card>
      </div>

      <Modal
        open={createOpen}
        title="Ajouter un employe"
        onClose={() => setCreateOpen(false)}
        actions={null}
      >
        <form className="form" onSubmit={createEmployee}>
          <div className="field">
            <div className="label">Matricule</div>
            <input
              className="input"
              value={createForm.matricule}
              onChange={(event) =>
                setCreateForm((prev) => ({
                  ...prev,
                  matricule: event.target.value,
                }))
              }
              maxLength={MATRICULE_MAX_LENGTH}
              required
            />
          </div>
          <div className="grid2">
            <div className="field">
              <div className="label">Nom</div>
              <input
                className="input"
                value={createForm.nom}
                onChange={(event) =>
                  setCreateForm((prev) => ({
                    ...prev,
                    nom: event.target.value,
                  }))
                }
                maxLength={FIELD_MAX_LENGTH}
                required
              />
            </div>
            <div className="field">
              <div className="label">Prenom</div>
              <input
                className="input"
                value={createForm.prenom}
                onChange={(event) =>
                  setCreateForm((prev) => ({
                    ...prev,
                    prenom: event.target.value,
                  }))
                }
                maxLength={FIELD_MAX_LENGTH}
                required
              />
            </div>
          </div>
          <div className="grid2">
            <div className="field">
              <div className="label">Poste</div>
              <input
                className="input"
                value={createForm.poste}
                onChange={(event) =>
                  setCreateForm((prev) => ({
                    ...prev,
                    poste: event.target.value,
                  }))
                }
                maxLength={FIELD_MAX_LENGTH}
                required
              />
            </div>
            <div className="field">
              <div className="label">Departement</div>
              <input
                className="input"
                value={createForm.departement}
                onChange={(event) =>
                  setCreateForm((prev) => ({
                    ...prev,
                    departement: event.target.value,
                  }))
                }
                maxLength={FIELD_MAX_LENGTH}
                required
              />
            </div>
          </div>
          <button className="btn btnPrimary" type="submit">
            Ajouter
          </button>
        </form>
      </Modal>

      <Modal
        open={editOpen}
        title="Modifier l'employe"
        onClose={() => setEditOpen(false)}
        actions={
          <button className="btn btnPrimary" onClick={saveEdit}>
            Enregistrer
          </button>
        }
      >
        <div className="form">
          <div className="field">
            <div className="label">Matricule</div>
            <input
              className="input"
              value={editForm.matricule}
              onChange={(event) =>
                setEditForm((prev) => ({
                  ...prev,
                  matricule: event.target.value,
                }))
              }
              maxLength={MATRICULE_MAX_LENGTH}
              required
            />
          </div>
          <div className="grid2">
            <div className="field">
              <div className="label">Nom</div>
              <input
                className="input"
                value={editForm.nom}
                onChange={(event) =>
                  setEditForm((prev) => ({ ...prev, nom: event.target.value }))
                }
                maxLength={FIELD_MAX_LENGTH}
                required
              />
            </div>
            <div className="field">
              <div className="label">Prenom</div>
              <input
                className="input"
                value={editForm.prenom}
                onChange={(event) =>
                  setEditForm((prev) => ({
                    ...prev,
                    prenom: event.target.value,
                  }))
                }
                maxLength={FIELD_MAX_LENGTH}
                required
              />
            </div>
          </div>
          <div className="grid2">
            <div className="field">
              <div className="label">Poste</div>
              <input
                className="input"
                value={editForm.poste}
                onChange={(event) =>
                  setEditForm((prev) => ({
                    ...prev,
                    poste: event.target.value,
                  }))
                }
                maxLength={FIELD_MAX_LENGTH}
                required
              />
            </div>
            <div className="field">
              <div className="label">Departement</div>
              <input
                className="input"
                value={editForm.departement}
                onChange={(event) =>
                  setEditForm((prev) => ({
                    ...prev,
                    departement: event.target.value,
                  }))
                }
                maxLength={FIELD_MAX_LENGTH}
                required
              />
            </div>
          </div>
        </div>
      </Modal>
      {confirmationDialog}
    </div>
  );
}
