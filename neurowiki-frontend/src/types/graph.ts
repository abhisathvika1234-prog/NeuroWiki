export interface GraphNodeData {
  id: string;
  label: string;
  type: string;
  sourceId: number | null;
  sourceType: string | null;
}

export interface GraphEdgeData {
  id: string;
  source: string;
  target: string;
  relationship: string;
}

export interface GraphResponseData {
  nodes: GraphNodeData[];
  edges: GraphEdgeData[];
}