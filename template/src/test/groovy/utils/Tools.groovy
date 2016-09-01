package utils

import spock.lang.*
import groovy.json.JsonSlurper
import cloud.artik.cloudconnector.api_v1.*

class Tools {
    static def parser = new JsonSlurper()

    /**
     * Read content in test/resources/responses/<verb>.<path with / replaced by _>.<num>.json
     * @param req the inpu request (from where verb and path is extracted)
     * @param num number of the reponse for the request (defaul "01")
     * @return the reponse read from resources
     */
    static def readResponse(req, num = "01") {
        def fpath = "/responses/" + req.method.name().toUpperCase() + "." + new URI(req.url).getPath().replace('/', '_').substring(1) +"." + num + ".json"
        def msg = Tools.class.getResource(fpath).getText('UTF-8')
        def fetchedResponse = new Response(HttpURLConnection.HTTP_OK, "application/json", msg)
    }

    static def readFile(Object caller, String path) {
        //def base = caller.getClass().getPackage().getName().replace('.', '/')
        //caller.getClass().getResource("/"+ base + "/" + path).getText('UTF-8')
        caller.getClass().getResource(path).getText('UTF-8')
    }

    static def cmpEvents(Collection<Event> l1, Collection<Event> l2) {
        eventsPrepareToCmp(l1) == eventsPrepareToCmp(l2)
    }

    static def tryConvertEventToEaseCmp(Event e) {
        if (e.kind == EventType.data) {
            try {
                return [e.ts, parser.parseText(e.payload)]
            } catch(Exception exc) {
                // ignore => default return
            }
        }
        return e
    }

    static def eventsPrepareToCmp(Collection<Event> l) {
        l.collect{tryConvertEventToEaseCmp(it)}.sort()
    }
}
