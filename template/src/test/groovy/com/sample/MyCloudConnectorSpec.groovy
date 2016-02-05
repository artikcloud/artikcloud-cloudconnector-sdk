package com.sample

import static java.net.HttpURLConnection.*

//import org.junit.Test
import spock.lang.*
import org.scalactic.*
import scala.Option
import org.joda.time.format.DateTimeFormat
import org.joda.time.*
import com.samsung.sami.cloudconnector.api_v1.*
import utils.FakeContext

class MyCloudConnectorSpec extends Specification {

		def sut = new MyCloudConnector()
		def ctx = new FakeContext()

		def "reject Notification without NotificationId"() {
			when:
			def req = new RequestDef("https://foo/cloudconnector/dt00/thirdpartynotification")
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
				new ThirdPartyNotification(new BySamiDeviceId(did), [
					new RequestDef("${ctx.parameters()['endpoint']}/messages/m1"),
					new RequestDef("${ctx.parameters()['endpoint']}/messages/m2")
				])
			])
		}

		def "send action to cloud when receiving SAMI action"() {
			when:
			def action = new ActionDef("sdid", "ddid", System.currentTimeMillis(), "setValue", '{"value":"foo"}')
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
					new BySamiDeviceId("ddid"), 
					[
						new RequestDef("${ctx.parameters().endpoint}/actions/extId/setValue")
							.withMethod(HttpMethod.Post)
							.withContent('{"value":"foo"}', "application/json")
					],
					[]
                )	
			])
		}

		def "reject unkown action"() {
			when:
			def action = new ActionDef("sdid", "ddid", System.currentTimeMillis(), "bar", '{"value":"foo"}')
			def fakeDevice = new DeviceInfo(
				"ddid", 
				Option.apply("extId"), 
				new Credentials(AuthType.OAuth2, "", "", Empty.option(), Option.apply("bearer"), ctx.scope(), Empty.option()), ctx.cloudId(), Empty.option()
			)
			def actionRes = sut.onAction(ctx, action, fakeDevice)

			then:
			actionRes.isBad()
		}
}
