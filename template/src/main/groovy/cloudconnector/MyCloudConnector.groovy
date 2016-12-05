// Sample CloudConnector, that can be used as a boostrap to write a new CloudConnector.
// Lot of code is commented, and everything is optional.
// The class can be named as you want no additional import allowed
// See the javadoc/scaladoc of cloud.artik.cloudconnector.api_v1.CloudConnector
package cloudconnector

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
        switch (phase) {
          case Phase.subscribe:
          case Phase.unsubscribe:
          case Phase.fetch:
            return new Good(req.addHeaders(["Authorization": "Bearer " + info.credentials.token]))
          // case Phase.getOauth2Code:
          // case Phase.getOauth2Token:
          // case Phase.refreshToken:
          // case Phase.undef:
          default:
            return super.signAndPrepare(ctx, req, info, phase)
        }
    }

    //@Override
    //Or<RequestDef, Failure> normalizeOauth2Code(Context ctx, RequestDef req) {
    //    new Good(req)
    //}

    //@Override
    //Or<RequestDef, Failure> normalizeOauth2Token(Context ctx, Response resp, Phase phase) {
    //    new Good(resp)
    //}

    // @Override
    // Or<CustomAuthenticationTask, Failure> onCustomAuthentication(Context ctx, RequestDef req, Option<Response> resp) {
    //     ctx.debug(req)
    //     if (resp.isDefined()) {
    //         // TODO process response of request to 3rd party cloud if needed
    //     } else  if (req.method() == HttpMethod.Get) {
    //         // start auth (display user form - first time)
    //         return new Good(new UserFormRequest("Test custom auth", [
    //             new InputField("username", "username"),
    //             new InputField("password", "password").withInputType("password")
    //         ]))
    //     } else {
    //         // handle user form
    //         def jsonForm = slurper.parseText(req.content())
    //         def user = jsonForm?.username?.get(0)
    //         def pwd = jsonForm?.password?.get(0)
    //         // In normal use case, call external cloud with a RequestDef and handle the response with Option<Response>
    //         // Here, we hardcode credentials
    //         if (user == "user" && pwd == "password") {
    //             return new Good(new CustomCredentials("myToken"))
    //         } else {
    //             return new Good(new UserFormRequest("Test custom auth", [
    //                 new InputField("username", "username").withError("Invalid username or password"),
    //                 new InputField("password", "password").withInputType("password").withError("Invalid username or password")
    //             ]))
    //         }
    //     }
    // }

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
    // CALLBACK & ACTION
    // -----------------------------------------
    @Override
    Or<NotificationResponse, Failure> onNotification(Context ctx, RequestDef req) {
        ctx.debug("onNotification: " + req)
        if (req.url.endsWith("thirdpartynotifications/postsubscription")) {
            def did = slurper.parseText(req.content())?.did
            //return new Good(new NotificationResponse([new ThirdPartyNotification(new ByDid(did), [])]))
            return new Good(new NotificationResponse([]))
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
            return new Good(new NotificationResponse([new ThirdPartyNotification(new ByDid(did), dataToFetch)]))
        } else {
            // nothing todo
            return new Good(new NotificationResponse([]))
        }
    }

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
                    if (req.url.startsWith("${ctx.parameters().endpoint}/messages/")) {
                        def json = slurper.parseText(content)
                        def ts = (json.datetime) ? Instant.from(ZonedDateTime.parse(json.datetime, mdateFormat)).toEpochMilli() : ctx.now()
                        //return new Good([new Event(ts, JsonOutput.toJson(slurper.parseText(json.message)))])
                        return new Good([new Event(ts, json.message)])
                    } else if (req.url.startsWith("${ctx.parameters().endpoint}/actions/")) {
                        // ignore response to action
                        return new Good([])
                    }
                    return new Bad(new Failure("unsupported request ${req.url}"))
                }
                return new Bad(new Failure("unsupported response ${res} ... ${res.contentType} .. ${res.contentType.startsWith(CT_JSON)}"))
            default:
                return new Bad(new Failure("http status : ${res.status} on ${req.method} ${req.url}, with content : ${res.content}"))
        }
    }

    // Or<List<Event>, Failure> onNotificationData(Context ctx, DeviceInfo info, String data) {
    //     new Bad(new Failure("unsupported: method onNotificationData should be implemented"))
    // }

    @Override
    Or<ActionResponse, Failure> onAction(Context ctx, ActionDef action, DeviceInfo info) {
        ctx.debug(action.extSubDeviceId)
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
    //   new Bad(new Failure("unsupported: method onActionData should be implemented"))
    // }
}
