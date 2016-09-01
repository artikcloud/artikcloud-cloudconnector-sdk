// Sample CloudConnector, that can be used as a boostrap to write a new CloudConnector.
// Every code is commented, because everything is optional.
// The class can be named as you want no additional import allowed
// See the javadoc/scaladoc of cloud.artik.cloudconnector.api_v1.CloudConnector
package com.sample

import static java.net.HttpURLConnection.*

import org.scalactic.*
import groovy.transform.CompileStatic
import groovy.transform.ToString
import groovy.json.JsonSlurper
import groovy.json.JsonOutput
import java.time.*
import java.time.format.DateTimeFormatter
import cloud.artik.cloudconnector.api_v1.*
import scala.Option

//@CompileStatic
class MyCloudConnector extends CloudConnector {
    static mdateFormat = DateTimeFormatter.ofPattern('yyyy-MM-dd HH:mm:ss').withZone(ZoneOffset.UTC)
    static final CT_JSON = 'application/json'

    JsonSlurper slurper = new JsonSlurper()

    @Override
    Or<RequestDef, Failure> signAndPrepare(Context ctx, RequestDef req, DeviceInfo info, Phase phase) {
        ctx.debug('phase: ' + phase)
        //if (phase == Phase.refreshToken) {
        //   //TODO change some params
        //}
        new Good(req.addHeaders(['Authorization':'Bearer ' + info.credentials.token]))
    }

    //@Override
    //Or<RequestDef, Failure> normalizeOauth2Code(Context ctx, RequestDef req) {
    //    new Good(req)
    //}

    //@Override
    //Or<RequestDef, Failure> normalizeOauth2Token(Context ctx, Response resp, Phase phase) {
    //    Good(resp)
    //}

    //@Override
    //Or<CustomAuthenticationTask, Failure> onCustomAuthentication(Context ctx, RequestDef req, Option<Response> resp) {
    //  new Bad(new Failure("unsupported: method onCustomAuthentication should be implemented"))
    //}

    // -----------------------------------------
    // SUBSCRIPTION
    // -----------------------------------------

    @Override
    Or<List<RequestDef>, Failure> subscribe(Context ctx, DeviceInfo info) {
        new Good([new RequestDef("${ctx.parameters().endpoint}/subscribe").withMethod(HttpMethod.Post).withContent('subscriptionId=' + info.did(), 'text/plain')])
    }

    // @Override
    // Or<Option<DeviceInfo>,Failure> onSubscribeResponse(Context ctx, RequestDef req,  DeviceInfo info, Response res) {
    //   def json = slurper.parseText(res.content)
    //   new Good(Option.apply(info.withExtId(json.userId)))
    // }

    @Override
    Or<List<RequestDef>, Failure> unsubscribe(Context ctx, DeviceInfo info) {
        new Good([new RequestDef("${ctx.parameters().endpoint}/unsubscribe").withMethod(HttpMethod.Get)])
    }

    // @Override
    // Or<Option<DeviceInfo>,Failure> onUnsubscribeResponse(Context ctx, RequestDef req,  DeviceInfo info, Response res) {
    //   new Good(Empty.option())
    // }

    // -----------------------------------------
    // CALLBACK
    // -----------------------------------------
    @Override
    Or<NotificationResponse, Failure> onNotification(Context ctx, RequestDef req) {
        ctx.debug("onNotification: " + req)
        if (req.url.endsWith("thirdpartynotifications/postsubscription")) {
            def did = slurper.parseText(req.content())?.did
            //return new Good(new NotificationResponse([new ThirdPartyNotification(new ByDeviceId(did), [])]))
            return Good(new NotificationResponse([]))
        } else if (req.contentType() == CT_JSON && req.content().trim().length() > 0) {
            def did = req.headers()['notificationId']
            if (did == null) {
                ctx.debug('Bad notification (where is did in following req : ) ' + req)
                return new Bad(new Failure('Impossible to recover device id from token request.'))
            }
            def content = req.content()
            def json = slurper.parseText(content)

            def dataToFetch = json.messages.collect { e ->
                new RequestDef("${ctx.parameters().endpoint}/messages/${e}")
            }
            return new Good(new NotificationResponse([new ThirdPartyNotification(new ByDeviceId(did), dataToFetch)]))
        } else {
            // nothing todo
            return new Good(new NotificationResponse([]))
        }
    }

    // @Override
    // Or<RequestDef, Failure> fetch(Context ctx, RequestDef req, DeviceInfo info) {
    //    new Good(req.addQueryParams(['userId' : info.extId().getOrElse(null)]))
    // }

    @Override
    Or<List<Event>, Failure> onFetchResponse(Context ctx, RequestDef req, DeviceInfo info, Response res) {
        switch (res.status) {
            case HTTP_OK:
                def content = res.content.trim()
                ctx.debug(content)
                if (content == '' || content == 'OK') {
                    ctx.debug("ignore response valid respond: '${res.content}'")
                    return new Good([])
                } else if (res.contentType.startsWith(CT_JSON)) {
                    def json = slurper.parseText(content)
                    def ts = (json.datetime) ? Instant.from(ZonedDateTime.parse(json.datetime, mdateFormat)).toEpochMilli() : ctx.now()
                    //return new Good([new Event(ts, JsonOutput.toJson(slurper.parseText(json.message)))])
                    return new Good([new Event(ts, json.message)])
                }
                return new Bad(new Failure("unsupported response ${res} ... ${res.contentType} .. ${res.contentType.startsWith(CT_JSON)}"))
            default:
                return new Bad(new Failure("http status : ${res.status} is not OK (${HTTP_OK}) on ${req.method} ${req.url}"))
        }
    }

    // Or<List<Event>, Failure> onNotificationData(Context ctx, DeviceInfo info, String data) {
    //     new Bad(new Failure("unsupported: method onNotificationData should be implemented"))
    // }

    @Override
    Or<ActionResponse, Failure> onAction(Context ctx, ActionDef action, DeviceInfo info) {
        switch (action.name) {
            case "setValue":
                def did = info.did
                def extId = info.extId.getOrElse(null)
                def paramsAsJson = slurper.parseText(action.params)
                def valueToSend = paramsAsJson.value

                if (extId == null) {
                    return new Bad(new Failure("Missing field external id in DeviceInfo ${extId}"))
                }
                if (valueToSend == null) {
                    return new Bad(new Failure("Missing field 'value' in action parameters ${paramsAsJson}"))
                }

                def req = new RequestDef("${ctx.parameters().endpoint}/actions/${extId}/setValue")
                              .withMethod(HttpMethod.Post)
                              .withContent("""{"value":"${valueToSend}"}""", CT_JSON)
                return new Good(new ActionResponse([new ActionRequest([req])]))
            default:
                return new Bad(new Failure("Unknown action: ${action.name}"))
        }
    }

    // Or<List<Event>, Failure> onActionData(Context ctx, DeviceInfo info, String data) {
    //  new Bad(new Failure("unsupported: method onActionData should be implemented"))
    // }
}
