package cloudconnector

import static java.net.HttpURLConnection.*

//import org.junit.Test
import spock.lang.*
import org.scalactic.*
import scala.Option
import cloud.artik.cloudconnector.api_v1.*
import utils.FakeContext
import static utils.Tools.*

class MyCloudConnectorSpec extends Specification {

    def sut = new MyCloudConnector()
    def ctx = new FakeContext()

    def did = "did000"
    def info = new DeviceInfo(did, Option.apply(did),
        new Credentials(AuthType.OAuth2, "", "ACCESSTOKEN000", Empty.option(), Option.apply("bearer"), ctx.scope(), Empty.option()),
        ctx.cloudId(),
        Empty.option()
    )

    def "reject Notification without NotificationId"() {
      when:
      def req = new RequestDef("https://foo/cloudconnector/dt00/thirdpartynotification")
        .withContent("{}", "application/json")
        .withMethod(HttpMethod.Post)
      def res = sut.onNotification(ctx, req)
      then:
      res.isBad()
    }

    def "accept valid Notification"() {
      when:
      def did = 'xxxx'
      def req = new RequestDef('https://foo/cloudconnector/dt00/thirdpartynotification')
        .withHeaders(['notificationId': did])
        .withContent('{"messages":["m1", "m2"]}', 'application/json')
      def res = sut.onNotification(ctx, req)

      then:
      res.isGood()
      res.get() == new NotificationResponse([
        new ThirdPartyNotification(new ByDid(did), [
          new RequestDef("${ctx.parameters()['endpoint']}/messages/m1"),
          new RequestDef("${ctx.parameters()['endpoint']}/messages/m2")
        ])
      ])
    }

    def "send action to cloud when receiving ARTIK Cloud action"() {
      when:
      def action = new ActionDef(Option.apply("sdid"), "ddid", System.currentTimeMillis(), "setValue", '{"value":"foo"}')
      def fakeDevice = new DeviceInfo(
        "ddid",
        Option.apply("extId"),
        new Credentials(AuthType.OAuth2, "", "", Empty.option(), Option.apply("bearer"), ctx.scope(), Empty.option()), ctx.cloudId(), Empty.option()
      )
      def actionRes = sut.onAction(ctx, action, fakeDevice)

      then:
      actionRes.isGood()
      actionRes.get() == new ActionResponse([
        new ActionRequest(
          [
            new RequestDef("${ctx.parameters().endpoint}/actions/extId/setValue")
              .withMethod(HttpMethod.Post)
              .withContent('{"value":"foo"}', "application/json")
          ]
        )
      ])
    }

    def "reject unkown action"() {
      when:
      def action = new ActionDef(Option.apply("sdid"), "ddid", System.currentTimeMillis(), "bar", '{"value":"foo"}')
      def fakeDevice = new DeviceInfo(
        "ddid",
        Option.apply("extId"),
        new Credentials(AuthType.OAuth2, "", "", Empty.option(), Option.apply("bearer"), ctx.scope(), Empty.option()), ctx.cloudId(), Empty.option()
      )
      def actionRes = sut.onAction(ctx, action, fakeDevice)

      then:
      actionRes.isBad()
    }

    def "process response from /devices"() {
        when:
            def req = new RequestDef("${ctx.parameters()['endpoint']}/messages/eid00")
            def resp = readResponse(req) //content in test/resources/responses/<verb>.<path with / replaced by _>.<num "01" by default>.json
            def res = sut.onFetchResponse(ctx, req, info, resp)
        then:
            res.isGood()
            //res.get()[0] == new Event(1472223806000,"""hello""", EventType.data)
            cmpTasks(res.get(),[
                new Event(1472223806000, """hello""", EventType.data)
            ])
    }

}
