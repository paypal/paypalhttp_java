task :default => :test

configatron.product_name = "BraintreeHttp Java"

# Other tasks
def build
  CommandProcessor.command("./gradlew clean build test")
end

configatron.build_method = method(:build)

def validate_version_match()
  if package_version != @current_release.version
    Printer.fail("package version #{package_version} does not match changelog version #{@current_release.version}.")
    abort()
  end
	Printer.success("package version #{package_version} matches latest changelog version #{@current_release.version}.")
end

def publish_to_package_manager(version)
  CommandProcessor.command("./gradlew braintreehttp:uploadArchives", live_output=true)
  CommandProcessor.command("./gradlew braintreehttp:closeAndReleaseRepository", live_output=true)

  puts
  sleep 60

  CommandProcessor.command("./gradlew braintreehttp-testutils:uploadArchives", live_output=true)
  CommandProcessor.command("./gradlew braintreehttp-testutils:closeAndReleaseRepository", live_output=true)
end

# The method that publishes the project to the package manager.  Required.
configatron.publish_to_package_manager_method = method(:publish_to_package_manager)


def wait_for_package_manager(version)
  CommandProcessor.wait_for("wget -U \"non-empty-user-agent\" -qO- http://central.maven.org/maven2/com/braintreepayments/braintreehttp/#{version}/braintreehttp-#{version}.pom | cat")
end

# The method that waits for the package manager to be done.  Required.
configatron.wait_for_package_manager_method = method(:wait_for_package_manager)

# True if publishing the root repo to GitHub.  Required.
configatron.release_to_github = true


# List of items to confirm from the person releasing.  Required, but empty list is ok.
configatron.prerelease_checklist_items = []
configatron.custom_validation_methods = [
  method(:validate_version_match)
]

def test
  CommandProcess.command("./gradlew clean test")
end

def package_version
	File.open("build.gradle", 'r') do |f|
		f.each_line do |line|
			puts line
			if line.match (/version \'\d+.\d+.\d+'/)
				return line.strip.split('\'')[1]
			end
		end
	end
end

task :test do
  test
end

