package com.neurowiki.config;

import com.neurowiki.entity.KnowledgeDocument;
import com.neurowiki.entity.KnowledgePage;
import com.neurowiki.entity.User;
import com.neurowiki.repository.KnowledgeDocumentRepository;
import com.neurowiki.repository.KnowledgePageRepository;
import com.neurowiki.repository.UserRepository;
import com.neurowiki.service.GraphService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DataInitializer.class);

    private final UserRepository userRepository;
    private final KnowledgePageRepository knowledgePageRepository;
    private final KnowledgeDocumentRepository knowledgeDocumentRepository;
    private final GraphService graphService;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(
            UserRepository userRepository,
            KnowledgePageRepository knowledgePageRepository,
            KnowledgeDocumentRepository knowledgeDocumentRepository,
            GraphService graphService,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.knowledgePageRepository = knowledgePageRepository;
        this.knowledgeDocumentRepository = knowledgeDocumentRepository;
        this.graphService = graphService;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (userRepository.count() > 0) {
            logger.info("Database already seeded. Skipping initial data population.");
            return;
        }

        logger.info("Seeding initial NeuroWiki demo data...");

        User adminUser = User.builder()
                .username("admin")
                .email("admin@neurowiki.com")
                .password(passwordEncoder.encode("admin123"))
                .build();

        User savedUser = userRepository.save(adminUser);

        // Seed Knowledge Pages
        KnowledgePage page1 = KnowledgePage.builder()
                .title("Retrieval-Augmented Generation Architecture")
                .content("Retrieval-Augmented Generation (RAG) is a technique for enhancing AI outputs by querying external knowledge documents and passing relevant context chunks into vector embeddings and generative language models.")
                .category("Artificial Intelligence")
                .tags("RAG, VectorDB, LLM, Embeddings")
                .favorite(true)
                .user(savedUser)
                .build();
        knowledgePageRepository.save(page1);

        KnowledgePage page2 = KnowledgePage.builder()
                .title("Neural Knowledge Graph Topology")
                .content("Knowledge graphs organize entities and relationships as interconnected nodes and edges. Combined with semantic search, graph topologies allow graph traversal across complex domains.")
                .category("Graph Neural Networks")
                .tags("Graph, Topology, Nodes, Edges, GraphML")
                .favorite(true)
                .user(savedUser)
                .build();
        knowledgePageRepository.save(page2);

        KnowledgePage page3 = KnowledgePage.builder()
                .title("Vector Embeddings & Semantic Search")
                .content("High-dimensional vector space representations map tokens and documents into semantic vectors. Cosine similarity operations retrieve contextually relevant passages.")
                .category("Machine Learning")
                .tags("Vectors, Embeddings, Cosine, Indexing")
                .favorite(false)
                .user(savedUser)
                .build();
        knowledgePageRepository.save(page3);

        // Seed Knowledge Documents
        KnowledgeDocument doc1 = KnowledgeDocument.builder()
                .title("Deep Learning Architecture Manual.pdf")
                .type("PDF")
                .content("Deep learning architectures rely on multi-layer neural networks, transformer blocks, self-attention mechanics, and backpropagation for optimizing model weights.")
                .status("PROCESSED")
                .fileSize("2.4 MB")
                .conceptsExtractedCount(14)
                .user(savedUser)
                .build();
        knowledgeDocumentRepository.save(doc1);

        KnowledgeDocument doc2 = KnowledgeDocument.builder()
                .title("https://arxiv.org/abs/2305.14314 (RAG Paper)")
                .type("URL")
                .sourceUrl("https://arxiv.org/abs/2305.14314")
                .content("Paper on grounded response generation using dense passage retrieval and autoregressive neural networks for factual consistency.")
                .status("PROCESSED")
                .fileSize("URL Data")
                .conceptsExtractedCount(18)
                .user(savedUser)
                .build();
        knowledgeDocumentRepository.save(doc2);

        // Seed Knowledge Graph Nodes & Edges
        graphService.processAndExtractGraph(savedUser, "KNOWLEDGE", page1.getId(), page1.getTitle(), page1.getContent());
        graphService.processAndExtractGraph(savedUser, "KNOWLEDGE", page2.getId(), page2.getTitle(), page2.getContent());
        graphService.processAndExtractGraph(savedUser, "PDF", doc1.getId(), doc1.getTitle(), doc1.getContent());

        logger.info("Demo data successfully seeded for user 'admin'.");
    }
}
