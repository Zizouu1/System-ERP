/**
 * Custom React hooks for API calls
 * All hooks return { data, error, loading } pattern
 * This allows components to gracefully handle errors without try/catch
 */

import { useEffect, useState } from "react";
import { ApiError } from "./http";
import * as endpoints from "./endpoints";
import type {
  UserDto,
  CreateUserReq,
  UpdateUserReq,
  StockDep1Dto,
  IncomingMaterialDto,
  OutgoingDto,
  OutgoingReq,
  ProductDto,
  ProductType,
  GlobalStockDto,
  PsfProductionReq,
  QrLabelDto,
  NomenclatureDto,
  Page,
  CreateBomReq,
  UpdateBomReq,
  ExportDto,
  CreateExportReq,
  Dept2ProductionDto,
  Dept2CreateProductionReq,
  SimulationReq,
  SimulationResp,
} from "./types";

export type ApiHookResult<T> = {
  data: T | null;
  error: ApiError | null;
  loading: boolean;
};

export type ApiFunctionResult<T> = {
  data: T | null;
  error: string | null;
};

/**
 * Generic hook for GET requests
 * @param fetchFn - Function that returns a Promise<T>
 * @param deps - Dependencies array (typically empty for initial load)
 */
function useApiQuery<T>(
  fetchFn: () => Promise<T>,
  deps: unknown[] = [],
): ApiHookResult<T> {
  const [data, setData] = useState<T | null>(null);
  const [error, setError] = useState<ApiError | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    let cancelled = false;

    (async () => {
      setLoading(true);
      setError(null);
      try {
        const result = await fetchFn();
        if (!cancelled) {
          setData(result);
        }
      } catch (err) {
        if (!cancelled) {
          if (err instanceof ApiError) {
            setError(err);
          } else if (err instanceof Error) {
            setError(new ApiError(err.message, 0));
          } else {
            setError(new ApiError("Erreur inconnue", 0));
          }
        }
      } finally {
        if (!cancelled) {
          setLoading(false);
        }
      }
    })();

    return () => {
      cancelled = true;
    };
  }, deps);

  return { data, error, loading };
}

// ─── Auth ────────────────────────────────────────────────────────────────────

// ─── Users (Admin) ───────────────────────────────────────────────────────────

export function useListUsers(): ApiHookResult<UserDto[]> {
  return useApiQuery(() => endpoints.adminListUsers(), []);
}

export async function useCreateUser(
  body: CreateUserReq,
): Promise<ApiFunctionResult<UserDto>> {
  try {
    const data = await endpoints.adminCreateUser(body);
    return { data, error: null };
  } catch (err) {
    const message =
      err instanceof ApiError ? err.message : "Échec de la création de l'utilisateur";
    return { data: null, error: message };
  }
}

export async function useUpdateUser(
  id: number,
  body: UpdateUserReq,
): Promise<ApiFunctionResult<UserDto>> {
  try {
    const data = await endpoints.adminUpdateUser(id, body);
    return { data, error: null };
  } catch (err) {
    const message =
      err instanceof ApiError ? err.message : "Échec de la mise à jour de l'utilisateur";
    return { data: null, error: message };
  }
}

export async function useDeleteUser(
  id: number,
): Promise<ApiFunctionResult<void>> {
  try {
    await endpoints.adminDeleteUser(id);
    return { data: undefined, error: null };
  } catch (err) {
    const message =
      err instanceof ApiError ? err.message : "Échec de la suppression de l'utilisateur";
    return { data: null, error: message };
  }
}

// ─── Stock (Logistic) ────────────────────────────────────────────────────────

export function useLogisticStock(): ApiHookResult<StockDep1Dto[]> {
  return useApiQuery(() => endpoints.logisticStock(), []);
}

export function useLogisticIncomingHistory(): ApiHookResult<
  IncomingMaterialDto[]
> {
  return useApiQuery(() => endpoints.logisticIncomingHistory(), []);
}

export async function useLogisticIncoming(
  body: IncomingMaterialDto,
): Promise<ApiFunctionResult<Blob>> {
  try {
    const data = await endpoints.logisticIncoming(body);
    return { data, error: null };
  } catch (err) {
    const message =
      err instanceof ApiError
        ? err.message
        : "Échec de la création de l'entrée";
    return { data: null, error: message };
  }
}

export async function useLogisticGenerateQr(
  reference: string,
  quantity: number,
  lotNumber: string,
): Promise<ApiFunctionResult<Blob>> {
  try {
    const data = await endpoints.logisticGenerateQr(
      reference,
      quantity,
      lotNumber,
    );
    return { data, error: null };
  } catch (err) {
    const message =
      err instanceof ApiError ? err.message : "Échec de la génération du code QR";
    return { data: null, error: message };
  }
}

// ─── Outgoing ────────────────────────────────────────────────────────────────

export async function useCreateOutgoing(
  body: OutgoingReq,
): Promise<ApiFunctionResult<void>> {
  try {
    await endpoints.createOutgoing(body);
    return { data: undefined, error: null };
  } catch (err) {
    const message =
      err instanceof ApiError ? err.message : "Échec de la création de la sortie";
    return { data: null, error: message };
  }
}

export function useListOutgoing(): ApiHookResult<OutgoingDto[]> {
  return useApiQuery(() => endpoints.listOutgoing(), []);
}

// ─── PSF (Dept1) ────────────────────────────────────────────────────────────

export function usePsfStocks(): ApiHookResult<StockDep1Dto[]> {
  return useApiQuery(() => endpoints.psfStocks(), []);
}

export async function usePsfProduction(
  body: PsfProductionReq,
): Promise<ApiFunctionResult<QrLabelDto[]>> {
  try {
    const data = await endpoints.psfProduction(body);
    return { data, error: null };
  } catch (err) {
    const message =
      err instanceof ApiError ? err.message : "Échec de la création de la production PSF";
    return { data: null, error: message };
  }
}

// ─── Products ────────────────────────────────────────────────────────────────

export function useListProducts(): ApiHookResult<ProductDto[]> {
  return useApiQuery(() => endpoints.listProducts(), []);
}

export function useListProducibleProducts(
  type: ProductType,
): ApiHookResult<GlobalStockDto[]> {
  return useApiQuery(() => endpoints.listProducibleProducts(type), [type]);
}
// ─── BOM / Nomenclature ──────────────────────────────────────────────────────

export function useListBom(): ApiHookResult<Page<NomenclatureDto>> {
  return useApiQuery(() => endpoints.listBom(), []);
}

export async function useCreateBom(
  body: CreateBomReq,
): Promise<ApiFunctionResult<NomenclatureDto>> {
  try {
    const data = await endpoints.createBom(body);
    return { data, error: null };
  } catch (err) {
    const message =
      err instanceof ApiError ? err.message : "Échec de la création de la nomenclature";
    return { data: null, error: message };
  }
}

export async function useUpdateBom(
  id: number,
  body: UpdateBomReq,
): Promise<ApiFunctionResult<NomenclatureDto>> {
  try {
    const data = await endpoints.updateBom(id, body);
    return { data, error: null };
  } catch (err) {
    const message =
      err instanceof ApiError ? err.message : "Échec de la mise à jour de la nomenclature";
    return { data: null, error: message };
  }
}

export async function useDeleteBom(
  id: number,
): Promise<ApiFunctionResult<void>> {
  try {
    await endpoints.deleteBom(id);
    return { data: undefined, error: null };
  } catch (err) {
    const message =
      err instanceof ApiError ? err.message : "Échec de la suppression de la nomenclature";
    return { data: null, error: message };
  }
}

// ─── Exports ─────────────────────────────────────────────────────────────────

export function useListExports(): ApiHookResult<ExportDto[]> {
  return useApiQuery(() => endpoints.listExports(), []);
}

export async function useCreateExport(
  body: CreateExportReq,
): Promise<ApiFunctionResult<ExportDto>> {
  try {
    const data = await endpoints.createExport(body);
    return { data, error: null };
  } catch (err) {
    const message =
      err instanceof ApiError ? err.message : "Échec de la création de l'export";
    return { data: null, error: message };
  }
}

export async function useUpdateExport(
  id: number,
  body: CreateExportReq,
): Promise<ApiFunctionResult<ExportDto>> {
  try {
    const data = await endpoints.updateExport(id, body);
    return { data, error: null };
  } catch (err) {
    const message =
      err instanceof ApiError ? err.message : "Échec de la mise à jour de l'export";
    return { data: null, error: message };
  }
}

export async function useDeleteExport(
  id: number,
): Promise<ApiFunctionResult<void>> {
  try {
    await endpoints.deleteExport(id);
    return { data: undefined, error: null };
  } catch (err) {
    const message =
      err instanceof ApiError ? err.message : "Échec de la suppression de l'export";
    return { data: null, error: message };
  }
}

// ─── PF (Dept2) ─────────────────────────────────────────────────────────────

export function useDept2List(): ApiHookResult<Dept2ProductionDto[]> {
  return useApiQuery(() => endpoints.dept2List(), []);
}

export async function useDept2Create(
  body: Dept2CreateProductionReq,
): Promise<ApiFunctionResult<Dept2ProductionDto>> {
  try {
    const data = await endpoints.dept2Create(body);
    return { data, error: null };
  } catch (err) {
    const message =
      err instanceof ApiError
        ? err.message
        : "Échec de la création de la production Dept2";
    return { data: null, error: message };
  }
}

// ─── Simulation ──────────────────────────────────────────────────────────────

export async function useSimulationCheck(
  body: SimulationReq,
): Promise<ApiFunctionResult<SimulationResp>> {
  try {
    const data = await endpoints.simulationCheck(body);
    return { data, error: null };
  } catch (err) {
    const message =
      err instanceof ApiError ? err.message : "La vérification de simulation a échoué";
    return { data: null, error: message };
  }
}
