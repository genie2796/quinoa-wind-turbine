package org.acme;

import io.smallrye.mutiny.Multi;
import io.smallrye.reactive.messaging.kafka.Record;

import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.jboss.resteasy.reactive.RestStreamElementType;

import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import java.time.Duration;
import java.util.List;

import static org.acme.Utils.withPing;

@ApplicationScoped
@Path("power")
public class PowerResource {

    @Channel("power-in") Multi<Power> powerIn;
    @Channel("power-out") Emitter<Power> powerOut;

    // For statistics/leader boards to Kafka
    @Channel("user-actions-out") Emitter<Record<String, Integer>> userActionsOut;

    @Path("stream")
    @GET
    @Produces(MediaType.SERVER_SENT_EVENTS)
    @RestStreamElementType(MediaType.APPLICATION_JSON)
    public Multi<List<Power>> stream() {
                return withPing(powerIn.group().intoLists().every(Duration.ofMillis(20)), List.of(Power.PING));
    }

    @Path("")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public void generate(Power power) {
        if (power.destination() > 2) {
            throw new IllegalArgumentException("We only have 2 teams for now");
        }
        powerOut.send(power);
        
        // Sends action to leader board topic
        userActionsOut.send(Record.of(power.source(), power.quantity()));
    }

    public static record Power(int quantity, String source, int destination) {
        public static final Power PING = new Power(0, "ping", -1);

    }

}