import com.alibaba.dashscope.assistants.Assistant;
import dev.langchain4j.community.model.dashscope.QwenChatModel;
import dev.langchain4j.community.model.dashscope.QwenEmbeddingModel;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.loader.ClassPathDocumentLoader;
import dev.langchain4j.data.document.parser.TextDocumentParser;
import dev.langchain4j.data.document.splitter.DocumentByCharacterSplitter;
import dev.langchain4j.data.document.splitter.DocumentByRegexSplitter;
import dev.langchain4j.data.document.splitter.DocumentBySentenceSplitter;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.message.CustomMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import org.junit.jupiter.api.Test;

import java.util.List;

public class ELPTest {

    @Test
    public void test01() {
        Document document = ClassPathDocumentLoader.loadDocument("rag/terms-of-service.txt", new TextDocumentParser());

       /* DocumentByCharacterSplitter splitter = new DocumentByCharacterSplitter(
                90,         // 每段最长字数
                10                              // 自然语言最大重叠字数
        );*/

        /**/
        DocumentByRegexSplitter splitter = new DocumentByRegexSplitter(
                "\\n\\d+\\.",  // 匹配 "1. 标题" 格式
                "\n",            // 保留换行符作为段落连接符
                100,             // 每个段最多 500 字符
                20,               // 段间重叠 50 字符以保持连贯性
                new DocumentByCharacterSplitter(100,50)
        );

        /*DocumentBySentenceSplitter splitter = new DocumentBySentenceSplitter(
                500,
                30
        );*/

        List<TextSegment> segments = splitter.split(document);

        System.out.println(segments);

        QwenEmbeddingModel  embeddingModel= QwenEmbeddingModel.builder()
                .apiKey(System.getenv("ALI_AI_KEY"))
                .build();
        // 向量化
        List<Embedding> embeddings = embeddingModel.embedAll(segments).content();
        System.out.println(embeddings);

        InMemoryEmbeddingStore<TextSegment> embeddingStore = new InMemoryEmbeddingStore<>();
        embeddingStore.addAll(embeddings,segments);

        // 生成向量
        Response<Embedding> embed = embeddingModel.embed("退费费用");
        EmbeddingSearchRequest build = EmbeddingSearchRequest.builder()
                .queryEmbedding(embed.content())
                .maxResults(1)
                .build();
      /*  // 查询
        EmbeddingSearchResult<TextSegment> results = embeddingStore.search(build);
        for (EmbeddingMatch<TextSegment> match : results.matches()) {
            System.out.println(match.embedded().text() + ",分数为：" + match.score());

        }*/



        /*---------------------检索增强阶段---------------------------*/

        ChatLanguageModel model = QwenChatModel
                .builder()
                .apiKey(System.getenv("ALI_AI_KEY"))
                .modelName("qwen-max")
                .build();

        ContentRetriever contentRetriever = EmbeddingStoreContentRetriever.builder()
                .embeddingStore(embeddingStore)
                .embeddingModel(embeddingModel)
                .maxResults(1) // 最相似的5个结果
                .minScore(0.7) // 只找相似度在0.6以上的内容
                .build();

        XushuAI xushuAI = AiServices.builder(XushuAI.class)
                .chatLanguageModel(model)
                .contentRetriever(contentRetriever)
                .build();

        System.out.println(xushuAI.chat("退费费用"));


    }


    public interface XushuAI {
        String chat(String message);
    }
}
