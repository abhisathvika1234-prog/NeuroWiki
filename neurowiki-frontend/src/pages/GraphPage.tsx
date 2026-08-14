import React, { useEffect, useState } from 'react';

import {
  ReactFlow,
  Background,
  Controls,
  MiniMap,
  Node,
  Edge,
  useNodesState,
  useEdgesState,
} from '@xyflow/react';

import '@xyflow/react/dist/style.css';

import { graphService } from '../services/graphService';
import { documentService } from '../services/documentService';

import {
  GraphNodeData,
  GraphEdgeData,
} from '../types/graph';

import { KnowledgeDocument } from '../types/document';

import Card from '../components/Card';
import EmptyState from '../components/EmptyState';
import LoadingSpinner from '../components/LoadingSpinner';

import {
  Network,
  Search,
  RotateCcw,
  Info,
  FileText,
  Globe,
  ArrowLeft,
  Eye,
} from 'lucide-react';

import { useNavigate } from 'react-router-dom';

export const GraphPage: React.FC = () => {
  const navigate = useNavigate();

  // =========================================================
  // SOURCE LIST
  // =========================================================

  const [documents, setDocuments] =
    useState<KnowledgeDocument[]>([]);

  const [loadingDocuments, setLoadingDocuments] =
    useState(true);

  const [selectedDocument, setSelectedDocument] =
    useState<KnowledgeDocument | null>(null);

  // =========================================================
  // GRAPH STATE
  // =========================================================

  const [loadingGraph, setLoadingGraph] =
    useState(false);

  const [error, setError] =
    useState<string | null>(null);

  const [rawNodes, setRawNodes] =
    useState<GraphNodeData[]>([]);

  const [rawEdges, setRawEdges] =
    useState<GraphEdgeData[]>([]);

  const [nodes, setNodes, onNodesChange] =
    useNodesState<Node>([]);

  const [edges, setEdges, onEdgesChange] =
    useEdgesState<Edge>([]);

  const [searchTerm, setSearchTerm] =
    useState('');

  const [selectedNode, setSelectedNode] =
    useState<GraphNodeData | null>(null);

  // =========================================================
  // LOAD DOCUMENTS
  // =========================================================

  useEffect(() => {
    fetchDocuments();
  }, []);

  const fetchDocuments = async () => {
    setLoadingDocuments(true);
    setError(null);

    try {
      const response =
        await documentService.getAll();

      /*
       * Your documentService appears to return:
       *
       * {
       *   ok: boolean,
       *   data: KnowledgeDocument[],
       *   message?: string
       * }
       */

      if (response.ok && response.data) {
        setDocuments(response.data);
      } else {
        setError(
          response.message ||
            'Failed to load knowledge sources'
        );
      }

    } catch (err: any) {
      setError(
        err?.message ||
          'Error loading knowledge sources'
      );
    } finally {
      setLoadingDocuments(false);
    }
  };

  // =========================================================
  // LOAD GRAPH FOR ONE SOURCE
  // =========================================================

  const openGraph = async (
    document: KnowledgeDocument
  ) => {

    if (!document.id) {
      setError(
        'This source does not have a valid ID.'
      );
      return;
    }

    setSelectedDocument(document);
    setSelectedNode(null);
    setSearchTerm('');
    setLoadingGraph(true);
    setError(null);

    // Clear old graph immediately

    setNodes([]);
    setEdges([]);
    setRawNodes([]);
    setRawEdges([]);

    try {

      /*
       * IMPORTANT
       *
       * Backend endpoint:
       *
       * GET /api/graph/{sourceType}/{sourceId}
       *
       * Example:
       *
       * /api/graph/PDF/10
       *
       * Therefore we must send BOTH:
       *
       * document.type
       * document.id
       */

      const response =
        await graphService.getGraphBySource(
          document.type || 'DOCUMENT',
          document.id
        );

      /*
       * IMPORTANT
       *
       * graphService returns GraphResponse directly.
       *
       * Therefore DO NOT write:
       *
       * response.ok
       * response.data
       *
       * Instead use:
       *
       * response.nodes
       * response.edges
       */

      const graphNodes =
        response.nodes || [];

      const graphEdges =
        response.edges || [];

      setRawNodes(graphNodes);
      setRawEdges(graphEdges);

      layoutGraph(
        graphNodes,
        graphEdges
      );

    } catch (err: any) {

      setError(
        err?.message ||
          'Error loading graph for this source'
      );

    } finally {

      setLoadingGraph(false);
    }
  };

  // =========================================================
  // GRAPH LAYOUT
  // =========================================================

  const layoutGraph = (
    inputNodes: GraphNodeData[],
    inputEdges: GraphEdgeData[]
  ) => {

    if (
      !inputNodes ||
      inputNodes.length === 0
    ) {
      setNodes([]);
      setEdges([]);
      return;
    }

    /*
     * Find main/source node.
     *
     * The source node normally has a type
     * such as PDF, KNOWLEDGE, URL, etc.
     *
     * Concept nodes have type CONCEPT.
     */

    const mainNodeIndex =
      inputNodes.findIndex(
        (node) =>
          node.type !== 'CONCEPT'
      );

    const orderedNodes =
      [...inputNodes];

    /*
     * Put source node first.
     */

    if (mainNodeIndex > 0) {

      const [mainNode] =
        orderedNodes.splice(
          mainNodeIndex,
          1
        );

      orderedNodes.unshift(
        mainNode
      );
    }

    const mainNode =
      orderedNodes.length > 0
        ? orderedNodes[0]
        : null;

    const conceptNodes =
      orderedNodes.slice(1);

    // =======================================================
    // CENTER
    // =======================================================

    const centerX = 500;
    const centerY = 350;

    const radius =
      Math.max(
        220,
        conceptNodes.length * 55
      );

    // =======================================================
    // CREATE FLOW NODES
    // =======================================================

    const flowNodes: Node[] =
      orderedNodes.map(
        (n) => {

          const isMainNode =
            mainNode &&
            String(n.id) ===
              String(mainNode.id);

          let x = centerX;
          let y = centerY;

          if (!isMainNode) {

            const conceptIndex =
              conceptNodes.findIndex(
                (concept) =>
                  String(concept.id) ===
                  String(n.id)
              );

            const angle =
              (2 * Math.PI * conceptIndex) /
              Math.max(
                conceptNodes.length,
                1
              );

            x =
              centerX +
              radius *
                Math.cos(angle);

            y =
              centerY +
              radius *
                Math.sin(angle);
          }

          const isConcept =
            n.type === 'CONCEPT';

          const bgColor =
            isConcept
              ? '#1e293b'
              : '#2563eb';

          const borderColor =
            isConcept
              ? '#6366f1'
              : '#60a5fa';

          return {

            id: String(n.id),

            position: {
              x,
              y,
            },

            data: {

              label: n.label,

              type: n.type,

              rawNode: n,
            },

            style: {

              background:
                bgColor,

              color:
                '#ffffff',

              border:
                `2px solid ${borderColor}`,

              borderRadius:
                '12px',

              padding:
                '10px 16px',

              fontWeight:
                600,

              fontSize:
                '14px',

              boxShadow:
                '0 4px 12px rgba(0,0,0,0.3)',

              cursor:
                'pointer',

              minWidth:
                '120px',

              maxWidth:
                '220px',

              textAlign:
                'center' as const,
            },
          };
        }
      );

    // =======================================================
    // CREATE FLOW EDGES
    // =======================================================

    const flowEdges: Edge[] =
      inputEdges.map(
        (e) => ({

          id:
            String(e.id),

          source:
            String(e.source),

          target:
            String(e.target),

          label:
            e.relationship,

          animated:
            true,

          style: {

            stroke:
              '#818cf8',

            strokeWidth:
              2,
          },

          labelStyle: {

            fill:
              '#cbd5e1',

            fontWeight:
              600,

            fontSize:
              '11px',
          },

          labelBgStyle: {

            fill:
              '#0f172a',

            fillOpacity:
              0.8,
          },
        })
      );

    setNodes(
      flowNodes
    );

    setEdges(
      flowEdges
    );
  };

  // =========================================================
  // SEARCH
  // =========================================================

  const handleSearchChange = (
    e: React.ChangeEvent<HTMLInputElement>
  ) => {

    const value =
      e.target.value;

    setSearchTerm(value);

    /*
     * Reset opacity when search is empty.
     */

    if (!value.trim()) {

      setNodes(
        (previousNodes) =>
          previousNodes.map(
            (node) => ({

              ...node,

              style: {

                ...node.style,

                opacity:
                  1,
              },
            })
          )
      );

      return;
    }

    const term =
      value.toLowerCase();

    setNodes(
      (previousNodes) =>
        previousNodes.map(
          (node) => {

            const label =
              String(
                node.data?.label ||
                  ''
              );

            const matches =
              label
                .toLowerCase()
                .includes(term);

            return {

              ...node,

              style: {

                ...node.style,

                opacity:
                  matches
                    ? 1
                    : 0.2,
              },
            };
          }
        )
    );
  };

  // =========================================================
  // NODE CLICK
  // =========================================================

  const handleNodeClick = (
    _event: React.MouseEvent,
    node: Node
  ) => {

    const rawNode =
      node.data
        ?.rawNode as GraphNodeData;

    if (rawNode) {
      setSelectedNode(
        rawNode
      );
    }
  };

  // =========================================================
  // RESET GRAPH
  // =========================================================

  const handleResetView = () => {

    setSearchTerm('');

    setSelectedNode(null);

    layoutGraph(
      rawNodes,
      rawEdges
    );
  };

  // =========================================================
  // BACK TO SOURCE LIST
  // =========================================================

  const handleBackToSources = () => {

    setSelectedDocument(null);

    setRawNodes([]);

    setRawEdges([]);

    setNodes([]);

    setEdges([]);

    setSelectedNode(null);

    setSearchTerm('');

    setError(null);
  };

  // =========================================================
  // OPEN SOURCE
  // =========================================================

  const handleNavigateSource = (
    node: GraphNodeData
  ) => {

    if (!node.sourceId) {
      return;
    }

    const sourceType =
      String(
        node.sourceType || ''
      ).toUpperCase();

    /*
     * Knowledge page
     */

    if (
      sourceType ===
      'KNOWLEDGE'
    ) {

      navigate(
        `/knowledge/${node.sourceId}`
      );

      return;
    }

    /*
     * PDF / URL / DOCUMENT
     */

    navigate(
      '/documents'
    );
  };

  // =========================================================
  // SOURCE ICON
  // =========================================================

  const getSourceIcon = (
    document: KnowledgeDocument
  ) => {

    const type =
      String(
        document.type || ''
      ).toUpperCase();

    if (
      type === 'URL' ||
      document.sourceUrl
    ) {

      return (
        <Globe size={22} />
      );
    }

    return (
      <FileText size={22} />
    );
  };

  // =========================================================
  // RENDER
  // =========================================================

  return (

    <div className="page-container">

      {/* =====================================================
          HEADER
      ===================================================== */}

      <div className="page-header">

        <div>

          <h1 className="page-title">
            Interactive Knowledge Graph
          </h1>

          <p className="page-subtitle">
            Select a knowledge source to
            explore its individual
            concept graph.
          </p>

        </div>

      </div>

      {/* =====================================================
          ERROR
      ===================================================== */}

      {error && (

        <div
          className="alert alert-error margin-bottom-lg"
        >

          <span>
            {error}
          </span>

        </div>
      )}

      {/* =====================================================
          SOURCE LIST
      ===================================================== */}

      {!selectedDocument && (

        <>

          {loadingDocuments ? (

            <LoadingSpinner
              text="Loading knowledge sources..."
            />

          ) : documents.length === 0 ? (

            <EmptyState
              icon={Network}
              title="No knowledge sources available."
              description="Create a knowledge page or upload a document first."
            />

          ) : (

            <div
              style={{
                display:
                  'grid',

                gridTemplateColumns:
                  'repeat(auto-fill, minmax(300px, 1fr))',

                gap:
                  '18px',
              }}
            >

              {documents.map(
                (document) => (

                  <Card
                    key={
                      document.id
                    }
                    style={{
                      padding:
                        '20px',
                    }}
                  >

                    {/* SOURCE TOP */}

                    <div
                      style={{
                        display:
                          'flex',

                        alignItems:
                          'flex-start',

                        gap:
                          '14px',
                      }}
                    >

                      {/* ICON */}

                      <div
                        style={{
                          width:
                            '48px',

                          height:
                            '48px',

                          borderRadius:
                            '12px',

                          display:
                            'flex',

                          alignItems:
                            'center',

                          justifyContent:
                            'center',

                          background:
                            'rgba(99,102,241,0.15)',

                          color:
                            '#818cf8',

                          flexShrink:
                            0,
                        }}
                      >

                        {getSourceIcon(
                          document
                        )}

                      </div>

                      {/* SOURCE INFO */}

                      <div
                        style={{
                          minWidth:
                            0,

                          flex:
                            1,
                        }}
                      >

                        <h3
                          style={{
                            margin:
                              '0 0 7px 0',

                            fontSize:
                              '16px',

                            fontWeight:
                              600,

                            wordBreak:
                              'break-word',
                          }}
                        >

                          {
                            document.title
                          }

                        </h3>

                        <div
                          style={{
                            display:
                              'flex',

                            gap:
                              '8px',

                            flexWrap:
                              'wrap',
                          }}
                        >

                          <span
                            className="badge"
                            style={{
                              background:
                                '#334155',

                              color:
                                '#e2e8f0',

                              padding:
                                '4px 8px',

                              borderRadius:
                                '5px',

                              fontSize:
                                '11px',
                            }}
                          >

                            {
                              document.type ||
                              'DOCUMENT'
                            }

                          </span>

                          {document.status && (

                            <span
                              className="badge"
                              style={{
                                background:
                                  '#14532d',

                                color:
                                  '#bbf7d0',

                                padding:
                                  '4px 8px',

                                borderRadius:
                                  '5px',

                                fontSize:
                                  '11px',
                              }}
                            >

                              {
                                document.status
                              }

                            </span>

                          )}

                        </div>

                      </div>

                    </div>

                    {/* SOURCE URL */}

                    {document.sourceUrl && (

                      <div
                        style={{
                          marginTop:
                            '14px',

                          fontSize:
                            '12px',

                          color:
                            '#94a3b8',

                          wordBreak:
                            'break-all',
                        }}
                      >

                        {
                          document.sourceUrl
                        }

                      </div>

                    )}

                    {/* VIEW GRAPH */}

                    <button
                      onClick={() =>
                        openGraph(
                          document
                        )
                      }
                      className="btn btn-primary"
                      style={{
                        width:
                          '100%',

                        marginTop:
                          '16px',

                        display:
                          'flex',

                        alignItems:
                          'center',

                        justifyContent:
                          'center',

                        gap:
                          '7px',
                      }}
                    >

                      <Eye size={16} />

                      View Graph

                    </button>

                  </Card>
                )
              )}

            </div>
          )}

        </>
      )}

      {/* =====================================================
          SELECTED SOURCE GRAPH
      ===================================================== */}

      {selectedDocument && (

        <>

          {/* ===================================================
              SOURCE HEADER
          =================================================== */}

          <Card
            className="margin-bottom-md"
            style={{
              padding:
                '16px 20px',
            }}
          >

            <div
              style={{
                display:
                  'flex',

                alignItems:
                  'center',

                justifyContent:
                  'space-between',

                gap:
                  '15px',

                flexWrap:
                  'wrap',
              }}
            >

              <div
                style={{
                  display:
                    'flex',

                  alignItems:
                    'center',

                  gap:
                    '12px',

                  minWidth:
                    0,
                }}
              >

                {/* BACK BUTTON */}

                <button
                  onClick={
                    handleBackToSources
                  }
                  className="btn btn-secondary"
                  style={{
                    display:
                      'flex',

                    alignItems:
                      'center',

                    gap:
                      '6px',
                  }}
                >

                  <ArrowLeft
                    size={16}
                  />

                  Sources

                </button>

                {/* SOURCE TITLE */}

                <div
                  style={{
                    minWidth:
                      0,
                  }}
                >

                  <h2
                    style={{
                      margin:
                        0,

                      fontSize:
                        '18px',

                      fontWeight:
                        600,

                      wordBreak:
                        'break-word',
                    }}
                  >

                    {
                      selectedDocument.title
                    }

                  </h2>

                  <div
                    style={{
                      marginTop:
                        '4px',

                      fontSize:
                        '12px',

                      color:
                        '#94a3b8',
                    }}
                  >

                    {
                      selectedDocument.type ||
                      'DOCUMENT'
                    }

                    {selectedDocument.sourceUrl
                      ? ` • ${selectedDocument.sourceUrl}`
                      : ''}

                  </div>

                </div>

              </div>

            </div>

          </Card>

          {/* ===================================================
              GRAPH CONTROLS
          =================================================== */}

          <Card
            className="graph-controls-card margin-bottom-md"
          >

            <div
              style={{
                display:
                  'flex',

                gap:
                  '12px',

                alignItems:
                  'center',

                justifyContent:
                  'space-between',

                flexWrap:
                  'wrap',
              }}
            >

              {/* SEARCH */}

              <div
                style={{
                  display:
                    'flex',

                  gap:
                    '10px',

                  alignItems:
                    'center',

                  flex:
                    1,

                  minWidth:
                    '260px',
                }}
              >

                <div
                  style={{
                    position:
                      'relative',

                    width:
                      '100%',
                  }}
                >

                  <Search
                    size={18}
                    style={{
                      position:
                        'absolute',

                      left:
                        '12px',

                      top:
                        '50%',

                      transform:
                        'translateY(-50%)',

                      opacity:
                        0.6,
                    }}
                  />

                  <input
                    type="text"
                    className="form-input"
                    style={{
                      paddingLeft:
                        '38px',
                    }}
                    placeholder="Search concepts or nodes..."
                    value={
                      searchTerm
                    }
                    onChange={
                      handleSearchChange
                    }
                  />

                </div>

              </div>

              {/* RESET */}

              <button
                onClick={
                  handleResetView
                }
                className="btn btn-secondary"
                style={{
                  display:
                    'flex',

                  alignItems:
                    'center',

                  gap:
                    '6px',
                }}
              >

                <RotateCcw
                  size={16}
                />

                Reset View

              </button>

            </div>

          </Card>

          {/* ===================================================
              GRAPH
          =================================================== */}

          {loadingGraph ? (

            <LoadingSpinner
              text={`Building graph for ${selectedDocument.title}...`}
            />

          ) : rawNodes.length === 0 ? (

            <EmptyState
              icon={Network}
              title="No graph data found for this source."
              description="The source exists, but no concepts or relationships have been extracted yet."
            />

          ) : (

            <div
              style={{
                display:
                  'flex',

                gap:
                  '20px',

                flexWrap:
                  'wrap',
              }}
            >

              {/* =================================================
                  GRAPH CANVAS
              ================================================= */}

              <div
                className="graph-canvas-container"
                style={{
                  height:
                    '600px',

                  minHeight:
                    '500px',

                  width:
                    selectedNode
                      ? 'calc(100% - 320px)'
                      : '100%',

                  position:
                    'relative',

                  borderRadius:
                    '12px',

                  overflow:
                    'hidden',

                  border:
                    '1px solid var(--color-border, #334155)',

                  background:
                    '#090d16',
                }}
              >

                <ReactFlow
                  nodes={
                    nodes
                  }
                  edges={
                    edges
                  }
                  onNodesChange={
                    onNodesChange
                  }
                  onEdgesChange={
                    onEdgesChange
                  }
                  onNodeClick={
                    handleNodeClick
                  }
                  fitView
                >

                  <Background
                    color="#1e293b"
                    gap={16}
                    size={1}
                  />

                  <Controls />

                  <MiniMap
                    nodeColor={
                      (node) =>
                        node.data
                          ?.type ===
                        'CONCEPT'
                          ? '#6366f1'
                          : '#3b82f6'
                    }
                    style={{
                      background:
                        '#0f172a',
                    }}
                  />

                </ReactFlow>

              </div>

              {/* =================================================
                  NODE DETAILS
              ================================================= */}

              {selectedNode && (

                <Card
                  style={{
                    width:
                      '300px',

                    alignSelf:
                      'flex-start',
                  }}
                >

                  {/* HEADER */}

                  <div
                    style={{
                      display:
                        'flex',

                      justifyContent:
                        'space-between',

                      alignItems:
                        'center',

                      marginBottom:
                        '12px',
                    }}
                  >

                    <h3
                      style={{
                        margin:
                          0,

                        fontSize:
                          '16px',

                        fontWeight:
                          600,
                      }}
                    >

                      Node Details

                    </h3>

                    <button
                      onClick={() =>
                        setSelectedNode(
                          null
                        )
                      }
                      style={{
                        background:
                          'none',

                        border:
                          'none',

                        color:
                          '#94a3b8',

                        cursor:
                          'pointer',

                        fontSize:
                          '18px',
                      }}
                    >

                      ✕

                    </button>

                  </div>

                  {/* NODE TYPE */}

                  <div
                    style={{
                      marginBottom:
                        '12px',
                    }}
                  >

                    <span
                      className="badge"
                      style={{
                        background:
                          selectedNode.type ===
                          'CONCEPT'
                            ? '#4f46e5'
                            : '#2563eb',

                        color:
                          '#fff',

                        padding:
                          '4px 8px',

                        borderRadius:
                          '4px',

                        fontSize:
                          '12px',
                      }}
                    >

                      {
                        selectedNode.type
                      }

                    </span>

                  </div>

                  {/* NODE LABEL */}

                  <h4
                    style={{
                      margin:
                        '0 0 8px 0',

                      fontSize:
                        '18px',

                      color:
                        '#f8fafc',

                      wordBreak:
                        'break-word',
                    }}
                  >

                    {
                      selectedNode.label
                    }

                  </h4>

                  {/* SOURCE ID */}

                  {selectedNode.sourceId && (

                    <div
                      style={{
                        fontSize:
                          '12px',

                        color:
                          '#94a3b8',

                        marginTop:
                          '8px',
                      }}
                    >

                      Source ID:{" "}
                      {
                        selectedNode.sourceId
                      }

                    </div>

                  )}

                  {/* SOURCE TYPE */}

                  {selectedNode.sourceType && (

                    <div
                      style={{
                        fontSize:
                          '12px',

                        color:
                          '#94a3b8',

                        marginTop:
                          '4px',
                      }}
                    >

                      Source Type:{" "}
                      {
                        selectedNode.sourceType
                      }

                    </div>

                  )}

                  {/* OPEN SOURCE */}

                  {selectedNode.sourceId && (

                    <button
                      onClick={() =>
                        handleNavigateSource(
                          selectedNode
                        )
                      }
                      className="btn btn-primary"
                      style={{
                        width:
                          '100%',

                        marginTop:
                          '12px',

                        fontSize:
                          '13px',

                        display:
                          'flex',

                        alignItems:
                          'center',

                        justifyContent:
                          'center',

                        gap:
                          '6px',
                      }}
                    >

                      <Info
                        size={14}
                      />

                      Open Source

                    </button>

                  )}

                </Card>

              )}

            </div>
          )}

        </>
      )}

    </div>
  );
};

export default GraphPage;