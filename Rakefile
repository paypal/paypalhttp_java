require 'rake'

task :release => [:clean, :build, :test, :release_braintreehttp, :release_braintreehttp_testutils]

task :clean do
  "./gradlew clean"
end

task :build do
  "./gradlew build"
end

task :test do
  "./gradew test"
end

task :release do
  sh "./gradlew :braintreehttp:uploadArchives  :braintreehttp-testutils:uploadArchives"
  sh "./gradlew :braintreehttp:closeRepository :braintreehttp-testutils:closeRepository"

  puts "Sleeping for one minute to allow BraintreeHttp modules to close"
  sleep 60
  sh "./gradlew :braintreehttp:promoteRepository :braintreehttp-testutils:promoteRepository"

  puts "Sleeping for ten minutes to allow BraintreeHttp modules to be promoted"
  sleep 600
  puts "Braintreehttp has been released"
end
