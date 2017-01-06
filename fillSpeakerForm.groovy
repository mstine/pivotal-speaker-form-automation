@Grapes([
    @Grab("org.gebish:geb-core:1.0"),
    @Grab("org.seleniumhq.selenium:selenium-chrome-driver:2.52.0"),
    @Grab("org.seleniumhq.selenium:selenium-support:2.52.0"),
    @Grab('org.yaml:snakeyaml:1.17')
])
import geb.Browser
import org.yaml.snakeyaml.*
import org.yaml.snakeyaml.constructor.*
import groovy.transform.*

class Main {

  static void main(String... args) {
      def speakers = loadSpeakersFromYaml(args[0])
      fillForm(speakers)
  }

  static Speakers loadSpeakersFromYaml(String yamlFilePath) {
    def rawYaml = new File(yamlFilePath).text
    def rootConstructor = new Constructor(Speakers.class)

    def rootDescription = new TypeDescription(Speakers.class)
    rootDescription.putListPropertyType("speakers", Speaker.class)
    rootConstructor.addTypeDescription(rootDescription)

    def speakerDescription = new TypeDescription(Speaker.class)
    speakerDescription.putListPropertyType("events", Event.class)
    rootConstructor.addTypeDescription(speakerDescription)

    def yaml = new Yaml(rootConstructor)
    def speakers = yaml.load(rawYaml)
  }

  static void fillForm(Speakers speakers) {
    Browser.drive {
      go "https://docs.google.com/a/pivotal.io/forms/d/1mxpbrFHK25XjXj--vN6a1IV4ztTQBjuYiD6u0nIfYd4/viewform"

      assert title == "Pivotal Software, Inc. - Sign In"

      $("form").username() << "username"
      $("form").password() << "********"
      def signInButton = $("input", type: "submit")
      assert signInButton.value() == "Sign In"
      signInButton.click()

      waitFor { $("div.mfa-verify") }

      def pushButton = $("div.mfa-verify").find("input", type: "submit")
      assert pushButton.value() == "Send Push"
      pushButton.click()

      waitFor(20, 0.5) {
        title == "Speaking on Behalf of Pivotal"
      }

      speakers.speakers.each { speaker ->
        speaker.events.each { event ->
          if (event.publish) {
            $("form").find("input[aria-label='Full Name']").value(speaker.fullName)
            $("form").find("input[aria-label='Title']").value(speaker.title)

            def twitterPlusLinkedIn = null
            if (speaker.linkedIn) {
              twitterPlusLinkedIn = "${speaker.twitter} + ${speaker.linkedIn}"
            } else {
              twitterPlusLinkedIn = speaker.twitter
            }

            $("form").find("input[aria-label='Twitter Handle + LinkedIn Profile']").value(twitterPlusLinkedIn)

            event.topics.each { topic ->
              $("form").find("span", text: "${topic}").click()
            }

            $("form").find("textarea[aria-label='Event Name ']").value(event.name)
            $("form").find("textarea[aria-label='Event URL']").value(event.url)
            $("form").find("textarea[aria-label='Event City']").value(event.city)
            $("form").find("input[jsname='YPqjbf'][type='date']") << "${event.date}"
            $("form").find("textarea[aria-label='Bio - no more than 100 words (link if already exists)']").value(speaker.bio)
            $("form").find("input[aria-label='Speaker headshot (link to an existing headshot or google drive)']").value(speaker.headshot)

            $("div.freebirdFormviewerViewNavigationSubmitButton").click()

            waitFor {
              $("div.freebirdFormviewerViewResponseConfirmationMessage").text() == "Thank you for submitting your speaker information. We will update the pivotal.io website to share your upcoming speaking engagement! If you have questions, please email askmarketing@pivotal.io"
            }

            if (event != speaker.events.last()) {
              $("a", text: "Submit another response").click()
            }
          }
        }
        if (speaker != speakers.speakers.last()) {
          $("a", text: "Submit another response").click()
        }
      }
    }
  }
}

@ToString(includeNames=true)
class Event {
  String name
  boolean publish
  String url
  String city
  String date
  List<String> topics
}

@ToString(includeNames=true)
class Speaker {
  String fullName
  String title
  String twitter
  String linkedIn
  String bio
  String headshot
  List<Event> events
}

@ToString(includeNames=true)
class Speakers {
  List<Speaker> speakers
}
