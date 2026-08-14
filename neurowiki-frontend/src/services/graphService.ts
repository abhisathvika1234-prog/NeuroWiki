import { apiFetch } from "../api/apiClient";

export interface GraphNode {
  id: string;
  label: string;
  type: string;
  sourceId: number | null;
  sourceType: string | null;
}

export interface GraphEdge {
  id: string;
  source: string;
  target: string;
  relationship: string;
}

export interface GraphResponse {
  nodes: GraphNode[];
  edges: GraphEdge[];
}

async function getGraph(): Promise<GraphResponse> {
  const response = await apiFetch<GraphResponse>("/api/graph", {
    method: "GET",
  });

  if (!response.ok) {
    throw new Error(
      response.message || `Graph request failed with status ${response.status}`
    );
  }

  return response.data || {
    nodes: [],
    edges: [],
  };
}

async function getGraphBySource(
  sourceType: string,
  sourceId: number
): Promise<GraphResponse> {

  const normalizedSourceType = sourceType.trim().toUpperCase();

  console.log(
    "Loading graph:",
    normalizedSourceType,
    sourceId
  );

  const response = await apiFetch<GraphResponse>(
    `/api/graph/${encodeURIComponent(normalizedSourceType)}/${sourceId}`,
    {
      method: "GET",
    }
  );

  if (!response.ok) {

    if (response.status === 401 || response.status === 403) {
      throw new Error(
        "Access forbidden. Your JWT token may be missing or invalid."
      );
    }

    throw new Error(
      response.message ||
      `Graph request failed with status ${response.status}`
    );
  }

  return response.data || {
    nodes: [],
    edges: [],
  };
}

async function deleteGraphBySource(
  sourceType: string,
  sourceId: number
): Promise<void> {

  const normalizedSourceType = sourceType.trim().toUpperCase();

  const response = await apiFetch(
    `/api/graph/${encodeURIComponent(normalizedSourceType)}/${sourceId}`,
    {
      method: "DELETE",
    }
  );

  if (!response.ok) {

    if (response.status === 401 || response.status === 403) {
      throw new Error(
        "Access forbidden. Your JWT token may be missing or invalid."
      );
    }

    throw new Error(
      response.message ||
      `Delete failed with status ${response.status}`
    );
  }
}

export const graphService = {
  getGraph,
  getGraphBySource,
  deleteGraphBySource,
};

export default graphService;