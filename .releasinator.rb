task :default => :test

configatron.product_name = "BraintreeHttp Java"

# List of items to confirm from the person releasing.  Required, but empty list is ok.
configatron.prerelease_checklist_items = [
]

# The directory where all distributed docs are.  If not specified, the default is `.`.
# configatron.base_docs_dir = '.'

configatron.build_method = method(:build)

def publish_to_package_manager(version)
  abort("please implement publish_to_package_manager method")
  # task :release_braintreehttp do
  #   sh "./gradlew :braintreehttp:uploadArchives  :braintreehttp-testutils:uploadArchives"
  #   sh "./gradlew :braintreehttp:closeRepository"
  #
  #   puts "Sleeping for one minute to allow BraintreeHttp modules to close"
  #   sleep 60
  #   sh "./gradlew :braintreehttp:promoteRepository :braintreehttp-testutils:promoteRepository"
  #
  #   puts "Sleeping for ten minutes to allow BraintreeHttp modules to be promoted"
  #   sleep 600
  #   puts "Braintreehttp has been released"
  # end
end

# The method that publishes the project to the package manager.  Required.
configatron.publish_to_package_manager_method = method(:publish_to_package_manager)


def wait_for_package_manager(version)
  puts "I'm here!"
end

# The method that waits for the package manager to be done.  Required.
configatron.wait_for_package_manager_method = method(:wait_for_package_manager)

# True if publishing the root repo to GitHub.  Required.
configatron.release_to_github = false


# Other tasks
def build
  CommandProcess.command("./gradlew clean build test")
end

def test
  CommandProcess.command("./gradlew clean test")
end

task :test do
  test
end
