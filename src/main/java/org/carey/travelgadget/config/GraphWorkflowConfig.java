package org.carey.travelgadget.config;

import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.OverAllStateFactory;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy;
import org.carey.travelgadget.graph.TripGraphStateKeys;
import org.carey.travelgadget.graph.node.ItineraryGenerateNode;
import org.carey.travelgadget.graph.node.RagSearchNode;
import org.carey.travelgadget.graph.node.SaveTripNode;
import org.carey.travelgadget.graph.node.WebSearchNode;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static com.alibaba.cloud.ai.graph.StateGraph.END;
import static com.alibaba.cloud.ai.graph.StateGraph.START;
import static com.alibaba.cloud.ai.graph.action.AsyncNodeAction.node_async;

/**
 * 全国旅游行程 Agent：START → RAG → 联网 → DeepSeek → MySQL → END
 */
@Configuration
public class GraphWorkflowConfig {

    @Bean
    public StateGraph tripStateGraph(RagSearchNode ragSearchNode,
                                     WebSearchNode webSearchNode,
                                     ItineraryGenerateNode itineraryGenerateNode,
                                     SaveTripNode saveTripNode) throws Exception {

        OverAllStateFactory stateFactory = () -> {
            OverAllState state = new OverAllState();
            register(state, TripGraphStateKeys.SESSION_ID);
            register(state, TripGraphStateKeys.DESTINATION_ID);
            register(state, TripGraphStateKeys.DESTINATION_NAME);
            register(state, TripGraphStateKeys.DEPARTURE_CITY_ID);
            register(state, TripGraphStateKeys.DEPARTURE_CITY_NAME);
            register(state, TripGraphStateKeys.DAYS);
            register(state, TripGraphStateKeys.TRAVELERS);
            register(state, TripGraphStateKeys.BUDGET_TIER);
            register(state, TripGraphStateKeys.THEME);
            register(state, TripGraphStateKeys.CUSTOM_REQUIRE);
            register(state, TripGraphStateKeys.ARRIVAL_HUB);
            register(state, TripGraphStateKeys.ARRIVAL_HUB_LABEL);
            register(state, TripGraphStateKeys.TRANSPORT_MODE);
            register(state, TripGraphStateKeys.CITIES);
            register(state, TripGraphStateKeys.LOCAL_TRANSPORT_NOTE);
            register(state, TripGraphStateKeys.DEPARTURE_DATE);
            register(state, TripGraphStateKeys.TRANSPORT_PREFERENCE);
            register(state, TripGraphStateKeys.TRANSPORT_PREFERENCE_LABEL);
            register(state, TripGraphStateKeys.RAG_CONTEXT);
            register(state, TripGraphStateKeys.WEB_CONTEXT);
            register(state, TripGraphStateKeys.ITINERARY_JSON);
            register(state, TripGraphStateKeys.BUDGET_JSON);
            register(state, TripGraphStateKeys.TRIP_ID);
            register(state, TripGraphStateKeys.STATUS);
            register(state, TripGraphStateKeys.ERROR);
            return state;
        };

        return new StateGraph("Travel Gadget National Workflow", stateFactory)
                .addNode("rag_search", node_async(ragSearchNode))
                .addNode("web_search", node_async(webSearchNode))
                .addNode("itinerary_generate", node_async(itineraryGenerateNode))
                .addNode("save_trip", node_async(saveTripNode))
                .addEdge(START, "rag_search")
                .addEdge("rag_search", "web_search")
                .addEdge("web_search", "itinerary_generate")
                .addEdge("itinerary_generate", "save_trip")
                .addEdge("save_trip", END);
    }

    private void register(OverAllState state, String key) {
        state.registerKeyAndStrategy(key, new ReplaceStrategy());
    }

    @Bean
    public CompiledGraph tripGraph(StateGraph tripStateGraph) throws Exception {
        return tripStateGraph.compile();
    }
}
