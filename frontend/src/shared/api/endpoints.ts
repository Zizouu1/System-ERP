import { apiBlob, apiJson } from "./http";
import { API_URL } from "../config/env";
import type {
  LoginResponse,
  UserDto,
  CreateUserReq,
  UpdateUserReq,
  EmployeeDto,
  EmployeeLookupDto,
  EmployeeUpsertReq,
  StockDep1Dto,
  IncomingMaterialDto,
  IncomingMaterialReq,
  IncomingUpdateResp,
  OutgoingReq,
  OutgoingDto,
  ProductDto,
  ProductType,
  ProductUpsertReq,
  GlobalStockDto,
  GlobalStockUpdateReq,
  NomenclatureDto,
  CreateBomReq,
  UpdateBomReq,
  ExportDto,
  CreateExportReq,
  PsfProductionReq,
  PsfProductionDto,
  PsfProductionUpdateResp,
  QrLabelDto,
  Dept2ProductionDto,
  Dept2CreateProductionReq,
  OperationAuditEntryDto,
  OutgoingUpdateReq,
  SimulationReq,
  SimulationBatchReq,
  SimulationResp,
  StockDep1UpdateReq,
  ProductionDelayPredictionReq,
  ProductionDelayPredictionResp,
  ModelMetricsResp,
  AdminNotificationDto,
  Page,
} from "./types";

// Auth
export const login = (username: string, password: string) =>
  apiJson<LoginResponse>(`${API_URL}/auth/login`, {
    method: "POST",
    body: { username, password },
  });

// Users (Admin)
export const adminListUsers = () =>
  apiJson<UserDto[]>(`${API_URL}/admin/users/all`);

export const adminCreateUser = (body: CreateUserReq) =>
  apiJson<UserDto>(`${API_URL}/admin/users/register`, { method: "POST", body });

export const adminUpdateUser = (id: number, body: UpdateUserReq) =>
  apiJson<UserDto>(`${API_URL}/admin/users/${id}`, { method: "PUT", body });

export const adminDeleteUser = (id: number) =>
  apiJson<void>(`${API_URL}/admin/users/${id}`, { method: "DELETE" });

// Employees (Admin + lookup for production roles)
export const adminListEmployees = () =>
  apiJson<EmployeeDto[]>(`${API_URL}/api/employees`);

export const adminCreateEmployee = (body: EmployeeUpsertReq) =>
  apiJson<EmployeeDto>(`${API_URL}/api/employees`, { method: "POST", body });

export const adminUpdateEmployee = (id: number, body: EmployeeUpsertReq) =>
  apiJson<EmployeeDto>(`${API_URL}/api/employees/${id}`, {
    method: "PUT",
    body,
  });

export const adminDeleteEmployee = (id: number) =>
  apiJson<void>(`${API_URL}/api/employees/${id}`, { method: "DELETE" });

export const importEmployees = (rows: string[][]) =>
  apiJson<string>(`${API_URL}/api/employees/import`, {
    method: "POST",
    body: rows,
  });

export const countEmployees = () =>
  apiJson<number>(`${API_URL}/api/employees/count`);

export const lookupEmployees = (query = "") =>
  apiJson<EmployeeLookupDto[]>(
    `${API_URL}/api/employees/lookup?q=${encodeURIComponent(query)}`,
  );

// Stock Dep1 (Externe)
export const logisticStock = () =>
  apiJson<StockDep1Dto[]>(`${API_URL}/api/psf/externe/stocks`);

export const logisticIncoming = (body: IncomingMaterialReq) =>
  apiBlob(`${API_URL}/api/psf/externe/incoming`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(body),
  });

export const logisticIncomingHistory = () =>
  apiJson<IncomingMaterialDto[]>(`${API_URL}/api/psf/externe/incoming`);

export const logisticIncomingUpdate = (id: number, body: IncomingMaterialReq) =>
  apiJson<IncomingUpdateResp>(`${API_URL}/api/psf/externe/incoming/${id}`, {
    method: "PUT",
    body,
  });

export const logisticIncomingDelete = (id: number) =>
  apiJson<void>(`${API_URL}/api/psf/externe/incoming/${id}`, {
    method: "DELETE",
  });

export const logisticGenerateQr = (
  reference: string,
  quantity: number,
  lotNumber: string,
) =>
  apiBlob(
    `${API_URL}/api/psf/externe/qr/generate?reference=${encodeURIComponent(reference)}&quantity=${quantity}&lotNumber=${encodeURIComponent(lotNumber)}`,
  );

// Outgoing (shared)
export const createOutgoing = (body: OutgoingReq) =>
  apiJson<void>(`${API_URL}/api/outgoing`, { method: "POST", body });

export const listOutgoing = () =>
  apiJson<OutgoingDto[]>(`${API_URL}/api/outgoing`);

export const updateOutgoing = (id: number, body: OutgoingUpdateReq) =>
  apiJson<OutgoingDto>(`${API_URL}/api/outgoing/${id}`, {
    method: "PUT",
    body,
  });

export const deleteOutgoing = (id: number) =>
  apiJson<void>(`${API_URL}/api/outgoing/${id}`, { method: "DELETE" });

// PSF Interne (Dept1)
export const psfStocks = () =>
  apiJson<StockDep1Dto[]>(`${API_URL}/api/psf/interne/stocks`);

export const psfProduction = (body: PsfProductionReq) =>
  apiJson<QrLabelDto[]>(`${API_URL}/api/psf/interne/production`, {
    method: "POST",
    body,
  });

export const psfProductionHistory = () =>
  apiJson<PsfProductionDto[]>(`${API_URL}/api/psf/interne/production`);

export const psfProductionUpdate = (id: number, body: PsfProductionReq) =>
  apiJson<PsfProductionUpdateResp>(
    `${API_URL}/api/psf/interne/production/${id}`,
    {
      method: "PUT",
      body,
    },
  );

export const psfProductionDelete = (id: number) =>
  apiJson<void>(`${API_URL}/api/psf/interne/production/${id}`, {
    method: "DELETE",
  });

export const dep1StockUpdate = (id: number, body: StockDep1UpdateReq) =>
  apiJson<StockDep1Dto>(`${API_URL}/api/psf/externe/stocks/${id}`, {
    method: "PUT",
    body,
  });

export const dep1StockDelete = (id: number) =>
  apiJson<void>(`${API_URL}/api/psf/externe/stocks/${id}`, {
    method: "DELETE",
  });

// Product master
export const listProducts = () =>
  apiJson<ProductDto[]>(`${API_URL}/api/products`);

export const listProductsPaged = (page = 0, size = 100) =>
  apiJson<Page<ProductDto>>(
    `${API_URL}/api/products/paged?page=${page}&size=${size}`,
  );

export const listActiveProducts = () =>
  apiJson<ProductDto[]>(`${API_URL}/api/products/active`);

export const createProduct = (body: ProductUpsertReq) =>
  apiJson<ProductDto>(`${API_URL}/api/products`, {
    method: "POST",
    body,
  });

export const updateProduct = (id: number, body: ProductUpsertReq) =>
  apiJson<ProductDto>(`${API_URL}/api/products/${id}`, {
    method: "PUT",
    body,
  });

export const deleteProduct = (id: number) =>
  apiJson<void>(`${API_URL}/api/products/${id}`, {
    method: "DELETE",
  });

export const importProducts = (rows: string[][]) =>
  apiJson<string>(`${API_URL}/api/products/import`, {
    method: "POST",
    body: rows,
  });

// Global stock
export const listGlobalStock = () =>
  apiJson<GlobalStockDto[]>(`${API_URL}/api/production-stock/products`);

export const updateGlobalStock = (id: number, body: GlobalStockUpdateReq) =>
  apiJson<GlobalStockDto>(
    `${API_URL}/api/production-stock/products/record/${id}`,
    {
      method: "PUT",
      body,
    },
  );

export const deleteGlobalStock = (id: number) =>
  apiJson<void>(`${API_URL}/api/production-stock/products/record/${id}`, {
    method: "DELETE",
  });

export const listProducibleProducts = (type: ProductType) =>
  apiJson<GlobalStockDto[]>(
    `${API_URL}/api/production-stock/products/producible?type=${type}`,
  );

// BOM / Nomenclature
export const listBom = (page = 0, size = 100) =>
  apiJson<Page<NomenclatureDto>>(
    `${API_URL}/api/nomenclatures?page=${page}&size=${size}`,
  );

export const countBom = () =>
  apiJson<number>(`${API_URL}/api/nomenclatures/count`);

export const createBom = (body: CreateBomReq) =>
  apiJson<NomenclatureDto>(`${API_URL}/api/nomenclatures`, {
    method: "POST",
    body,
  });

export const updateBom = (id: number, body: UpdateBomReq) =>
  apiJson<NomenclatureDto>(`${API_URL}/api/nomenclatures/${id}`, {
    method: "PUT",
    body,
  });

export const deleteBom = (id: number) =>
  apiJson<void>(`${API_URL}/api/nomenclatures/${id}`, { method: "DELETE" });

export const listUniqueRefs = () =>
  apiJson<string[]>(`${API_URL}/api/nomenclatures/unique-refs`);

export const importNomenclature = (rows: string[][]) =>
  apiJson<string>(`${API_URL}/api/nomenclatures/import`, {
    method: "POST",
    body: rows,
  });

// Exports
export const listExports = () => apiJson<ExportDto[]>(`${API_URL}/api/exports`);

export const createExport = (body: CreateExportReq) =>
  apiJson<ExportDto>(`${API_URL}/api/exports`, { method: "POST", body });

export const updateExport = (id: number, body: CreateExportReq) =>
  apiJson<ExportDto>(`${API_URL}/api/exports/${id}`, { method: "PUT", body });

export const deleteExport = (id: number) =>
  apiJson<void>(`${API_URL}/api/exports/${id}`, { method: "DELETE" });

// PF (Dept2)
export const dept2List = () =>
  apiJson<Dept2ProductionDto[]>(`${API_URL}/api/production/dept2`);

export const dept2Create = (body: Dept2CreateProductionReq) =>
  apiJson<Dept2ProductionDto>(`${API_URL}/api/production/dept2`, {
    method: "POST",
    body,
  });

export const dept2Update = (id: number, body: Dept2CreateProductionReq) =>
  apiJson<Dept2ProductionDto>(`${API_URL}/api/production/dept2/${id}`, {
    method: "PUT",
    body,
  });

export const dept2Delete = (id: number) =>
  apiJson<void>(`${API_URL}/api/production/dept2/${id}`, {
    method: "DELETE",
  });

export const operationAuditHistory = (entityType: string, entityId: number) =>
  apiJson<OperationAuditEntryDto[]>(
    `${API_URL}/api/audit/operations/${encodeURIComponent(entityType.trim().toUpperCase())}/${entityId}`,
  );

// Admin notifications
export const adminListNotifications = () =>
  apiJson<AdminNotificationDto[]>(`${API_URL}/api/admin/notifications`);

export const adminUnreadNotificationsCount = () =>
  apiJson<{ count: number }>(`${API_URL}/api/admin/notifications/unread-count`);

export const adminMarkNotificationSeen = (id: number) =>
  (() => {
    const url = `${API_URL}/api/admin/notifications/${id}/seen`;
    console.debug("[notif-api-client] request", { method: "PATCH", url, id });
    return apiJson<AdminNotificationDto>(url, {
      method: "PATCH",
    });
  })();

export const adminMarkAllNotificationsSeen = () =>
  (() => {
    const url = `${API_URL}/api/admin/notifications/seen-all`;
    console.debug("[notif-api-client] request", { method: "PATCH", url });
    return apiJson<{ updated: number }>(url, {
      method: "PATCH",
    });
  })();

// Simulation
export const simulationCheck = (body: SimulationReq) =>
  apiJson<SimulationResp>(`${API_URL}/api/simulation/check`, {
    method: "POST",
    body,
  });

export const simulationCheckBatch = (body: SimulationBatchReq) =>
  apiJson<SimulationResp>(`${API_URL}/api/simulation/check-bulk`, {
    method: "POST",
    body,
  });

export const predictProductionDelay = (body: ProductionDelayPredictionReq) =>
  apiJson<ProductionDelayPredictionResp>(
    `${API_URL}/api/production-delay/predict`,
    {
      method: "POST",
      body,
    },
  );

export const fetchModelMetrics = () =>
  apiJson<ModelMetricsResp>(`${API_URL}/api/model-metrics`);
