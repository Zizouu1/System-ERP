// Sidebar role type (uppercase, as used by backend user management payloads)
export type ApiRole = "ADMIN" | "LOGISTIC" | "PSF" | "PF";

// JWT role strings as returned by the backend
export type JwtRole = "ROLE_ADMIN" | "ROLE_LOGISTIC" | "ROLE_PSF" | "ROLE_PF";

export type LoginResponse = { token: string };

// Admin - Users
export type UserDto = {
  id: number;
  username: string;
  role: string;
  firstname?: string;
  lastname?: string;
  matricule?: string;
};

export type CreateUserReq = {
  username: string;
  password: string;
  role: string;
  firstname?: string;
  lastname?: string;
  matricule?: string;
};

export type UpdateUserReq = {
  username?: string;
  role?: string;
  password?: string;
  firstname?: string;
  lastname?: string;
  matricule?: string;
};

// Employees
export type EmployeeDto = {
  id: number;
  matricule: string;
  nom: string;
  prenom: string;
  poste: string;
  departement: string;
};

export type EmployeeUpsertReq = {
  matricule: string;
  nom: string;
  prenom: string;
  poste: string;
  departement: string;
};

export type EmployeeLookupDto = {
  matricule: string;
  nom: string;
  prenom: string;
  poste: string;
  departement: string;
};

export type ProductType = "MATIERE_PREMIERE" | "SEMI_FINI" | "PRODUIT_FINI";

// Product master (/api/products)
export type ProductDto = {
  id: number;
  ref: string;
  designation: string;
  productType: ProductType;
  displayType?: string;
  active?: boolean;
};

export type ProductUpsertReq = {
  ref: string;
  designation: string;
  productType: ProductType;
  active?: boolean;
};

// Global stock (/api/production-stock/products)
export type GlobalStockDto = {
  id: number;
  ref: string;
  type: ProductType;
  quantityTotal?: number;
  quantityUsed?: number;
  quantityAvailable?: number;
};

export type GlobalStockUpdateReq = {
  quantityTotal: number;
  quantityUsed: number;
};

// Stock (Dept1)
export type StockDep1Dto = {
  id: number;
  reference: string;
  lotNumber?: string;
  productType: string;
  totalQuantity: number;
  storeQuantity: number;
  transferedQuantity: number;
};

export type StockDep1UpdateReq = {
  totalQuantity: number;
  storeQuantity: number;
};

// Incoming material (Logistic -> POST /api/psf/externe/incoming)
export type IncomingMaterialDto = {
  id?: number;
  reference: string;
  quantity: number;
  lotNumber: string;
  generateQr?: boolean;
  operationDate?: string;
  lastModifiedAt: string | null;
  lastModifiedBy?: string | null;
};

export type IncomingMaterialReq = {
  reference: string;
  quantity: number;
  lotNumber: string;
  generateQr?: boolean;
  operationDate?: string;
};

export type IncomingUpdateResp = {
  incoming: IncomingMaterialDto;
  qrImageBase64: string;
  reference: string;
  quantity: number;
  lotNumber: string;
  generatedAt: string;
};

// Outgoing (shared -> POST /api/outgoing)
export type OutgoingReq = {
  reference: string;
  lotNumber: string;
  quantityOut: number;
  operationDate?: string;
};

export type OutgoingDto = OutgoingReq & {
  id?: number;
  lastModifiedAt: string | null;
  lastModifiedBy?: string | null;
};

export type OutgoingUpdateReq = {
  reference: string;
  lotNumber: string;
  quantityOut: number;
  operationDate?: string;
};

// BOM / Nomenclature
export type NomenclatureDto = {
  id: number;
  parentRef: string;
  componentRef: string;
  quantityRequired: number;
};

export type CreateBomReq = {
  parentRef: string;
  componentRef: string;
  quantityRequired: number;
};

export type UpdateBomReq = Partial<CreateBomReq>;

// Exports
export type ExportDto = {
  id: number;
  product?: GlobalStockDto;
  quantity: number;
  exportDate?: string;
};

export type CreateExportReq = {
  reference: string;
  quantity: number;
};

// PSF (Dept1)
export type PsfProductionReq = {
  reference: string;
  quantity: number;
  quantityPerBatch: number;
  operatorMatricule: string;
  scrapQuantity: number;
  producedByCutMachine: boolean;
  startTime: string;
  endTime: string;
};

export type QrLabelDto = {
  reference: string;
  quantity: number;
  lotNumber: string;
  qrImageBase64: string;
  generatedAt: string;
};

export type PsfProductionDto = {
  id: number;
  reference: string;
  quantity: number;
  quantityPerBatch: number;
  batches: number;
  timestamp: string;
  producedByCutMachine: boolean;
  scrapQuantity: number;
  operatorMatricule: string;
  startTime: string;
  endTime: string;
  lastModifiedAt: string | null;
  lastModifiedBy?: string | null;
};

export type PsfProductionUpdateResp = {
  production: PsfProductionDto;
  qrLabels: QrLabelDto[];
};

// PF (Dept2)
export type Dept2ProductionDto = {
  id: number;
  operatorMatricule: string;
  reference: string;
  quantity: number;
  createdAt?: string;
  startTime: string;
  endTime: string;
  startDateTime?: string;
  endDateTime?: string;
  scrapQuantity?: number;
  lastModifiedAt: string | null;
  lastModifiedBy?: string | null;
};

export type Dept2CreateProductionReq = {
  operatorMatricule: string;
  reference: string;
  quantity: number;
  startTime: string;
  endTime: string;
  scrapQuantity?: number;
};

export type OperationAuditEntryDto = {
  id: number;
  entityType: string;
  entityId: number;
  changedByUsername: string;
  changedAt: string;
  oldValues: Record<string, unknown>;
  newValues: Record<string, unknown>;
};

export type AdminNotificationDto = {
  id: number;
  type: string;
  message: string;
  seen: boolean;
  createdAt: string;
  targetPath?: string;
  targetEntityType?: string;
  targetEntityId?: number;
  targetReference?: string;
  actor?: string;
};

// Simulation
export type SimulationReq = {
  productId?: number;
  reference?: string;
  quantity: number;
};

export type SimulationBatchReq = {
  items: SimulationReq[];
};

export type MissingItem = {
  reference: string;
  requiredQty: number;
  availableQty: number;
  missingQty: number;
  designation?: string;
  affectedProductRef?: string;
  affectedProductDesignation?: string;
  productContributions?: Array<{
    productRef: string;
    productDesignation?: string;
    producedQty?: number;
    requiredQty: number;
  }>;
};

export type RequiredItem = {
  reference: string;
  requiredQty: number;
  availableQty: number;
  missingQty: number;
};

export type SimulationResp = {
  possible: boolean;
  missingItems: MissingItem[];
  requiredItems?: RequiredItem[];
  message?: string;
};

export type ProductionDelayPredictionReq = {
  duree: number;
  quantiteCommandee: number;
  machinesDisponibles: number;
  bomDepth: number;
  totalOperations: number;
  totalBomComponents: number;
};

export type ProductionDelayPredictionResp = {
  delayProbability?: number;
  delay_probability?: number;
  prediction?: number;
  accuracy?: number;
  precision?: number;
  recall?: number;
  f1?: number;
  roc_auc?: number;
  rocAuc?: number;
  metrics?: {
    accuracy?: number;
    precision?: number;
    recall?: number;
    f1?: number;
    roc_auc?: number;
    rocAuc?: number;
  };
  message?: string;
  error?: string;
};

export type ModelMetricsResp = {
  accuracy: number | null;
  precision: number | null;
  recall: number | null;
  f1: number | null;
  roc_auc: number | null;
};

export type Page<T> = {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  last: boolean;
};
