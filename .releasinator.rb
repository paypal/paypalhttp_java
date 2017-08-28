task :default => :test

configatron.product_name = "BraintreeHttp Java"

# Custom validations
def package_version
	File.open("build.gradle", 'r') do |f|
		f.each_line do |line|
			if line.match (/version \'\d+.\d+.\d+'/)
				return line.strip.split('\'')[1]
			end
		end
	end
end

def validate_version_match
  if package_version != @current_release.version
    Printer.fail("package version #{package_version} does not match changelog version #{@current_release.version}.")
    abort()
  end
	Printer.success("package version #{package_version} matches latest changelog version #{@current_release.version}.")
end

def test
  CommandProcessor.command("./gradlew clean build test")
end

configatron.custom_validation_methods = [
  method(:validate_version_match),
  method(:test)
]

# Update version, build, and publish to Maven
def update_version_method(version, semver_type)
  contents = File.read("build.gradle")
  contents = contents.gsub(/version '\d+.\d+.\d+'/, "version '#{version}'")
  File.open("build.gradle", "w") do |file|
    file.puts contents
  end
end

configatron.update_version_method = method(:update_version_method)

def build
  CommandProcessor.command("./gradlew clean build")
end

configatron.build_method = method(:build)

def publish_to_package_manager(version)
  CommandProcessor.command("./gradlew braintreehttp:uploadArchives", live_output=true)
  CommandProcessor.command("./gradlew braintreehttp:closeAndReleaseRepository", live_output=true)

  puts
  sleep 60

  CommandProcessor.command("./gradlew braintreehttp-testutils:uploadArchives", live_output=true)
  CommandProcessor.command("./gradlew braintreehttp-testutils:closeAndReleaseRepository", live_output=true)
end

configatron.publish_to_package_manager_method = method(:publish_to_package_manager)

def wait_for_package_manager(version)
  CommandProcessor.wait_for("wget -U \"non-empty-user-agent\" -qO- http://central.maven.org/maven2/com/braintreepayments/braintreehttp/#{version}/braintreehttp-#{version}.pom | cat")
end

configatron.wait_for_package_manager_method = method(:wait_for_package_manager)

# Miscellania
configatron.release_to_github = true
configatron.prerelease_checklist_items = []
