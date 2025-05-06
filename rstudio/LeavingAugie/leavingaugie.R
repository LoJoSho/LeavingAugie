library(httr)
library(jsonlite)
library(tibble)
library(dplyr)
library(shiny)
library(ggplot2)
library(lubridate)
library(plotly)


rm(list = ls())

options(scipen = 999)

API_KEY <- ""

getLocations <- function() {
  r1 <- GET(paste0("localhost:8080/locations?apiKey=", API_KEY))

  location_text <- content(r1, "text", "UFT-8")
  parsed <- fromJSON(location_text)

  df <- enframe(parsed, name = "id", value = "location_info") %>%
    rowwise() %>%
    mutate(
      location = names(location_info),
      origin = as.logical(location_info[[1]])
    ) %>%
    select(id, location, origin)

  return(df)
}

df <- getLocations()
origins <- df %>%
  filter(origin == TRUE)
destinations <- df %>%
  filter(origin == FALSE)

getTimes <- function(originstring, destinationstring) {
  origin <- df %>% 
    filter(location == originstring) %>%
    pull(id)
  destination <- df %>% 
    filter(location == destinationstring) %>%
    pull(id)
  
  print(paste0("Attempting to get with origin ", origin, " and destination id ", destination))
  
  r1 <- GET(paste0("localhost:8080/fulldata?origin=", origin, "&destination=", destination, "&apiKey=", API_KEY))
  time_text <- content(r1, "text", "UFT-8")
  parsed <- fromJSON(time_text)
  
  times_summary <- data.frame(
    time = as.POSIXct(as.numeric(names(parsed)) / 1000, origin = "1970-01-01", tz = "America/Chicago"),
    seconds = as.numeric(parsed)
  )
  
  return(times_summary)
}

ui <- fluidPage(
  
  # Application title
  titlePanel("Leaving Augie Data"),
  
  # Sidebar with a slider input for number of bins 
  sidebarLayout(
    sidebarPanel(
      selectInput(inputId = "originid",
                    label = "Select Origin",
                    origins$location),
    selectInput(inputId = "destinationid",
                label = "Select Destination",
                destinations$location),
    textOutput("loc_mean"),
    textOutput("loc_median"),
    textOutput("loc_max"),
    textOutput("loc_min")
    ),
    
    # Show a plot of the generated distribution
    mainPanel(
        # Show data table
      #DT::DTOutput(outputId = "data")
      plotlyOutput( "timePlot")
    ),
  ),
    tags$hr(),
    mainPanel(
      DT::DTOutput(outputId = "data")
    )
)

# Define server logic required to draw a histogram
server <- function(input, output) {
  times <- reactive({
    gottenTime <- getTimes(input$originid, input$destinationid)
    
    times_summary <- gottenTime %>%
      mutate(
        weekday_num = lubridate::wday(time, week_start = 1),  # Monday = 1
        weekday = lubridate::wday(time, label = TRUE, abbr = FALSE, week_start = 1),
        minute_of_day = as.numeric(format(time, "%H")) * 60 + as.numeric(format(time, "%M")),
        datetime = as.POSIXct("2000-01-03", tz = "America/Chicago") + days(weekday_num - 1) + minutes(minute_of_day)
      ) %>%
      group_by(datetime, weekday) %>%
      summarise(avg_seconds = mean(seconds), .groups = "drop")
  })
  
  rawTimes <- reactive({
    gottenTime <- getTimes(input$originid, input$destinationid)
    
    times_summary <- gottenTime %>%
      mutate(
        weekday_num = lubridate::wday(time, week_start = 1),  # Monday = 1
        weekday = lubridate::wday(time, label = TRUE, abbr = FALSE, week_start = 1),
        minute_of_day = as.numeric(format(time, "%H")) * 60 + as.numeric(format(time, "%M")) 
      )
  })
  
  output$loc_mean <-renderText({
    total_seconds <- ((mean(times()$avg_seconds)))
    duration <- seconds_to_period(total_seconds)
    
    string <- paste(
      if (hour(duration) > 0) paste0(hour(duration), " hour", ifelse(hour(duration) == 1, "", "s")) else NULL,
      if (minute(duration) > 0) paste0(minute(duration), " minute", ifelse(minute(duration) == 1, "", "s")) else NULL,
      if (round(second(duration), 0) > 0) paste0(round(second(duration), 0), " second", ifelse(round(second(duration), 0) == 1, "", "s")) else NULL,
      sep = ", "
    )
    
     string <- paste0("Mean: ", string)
  })
  
  output$loc_median <-renderText({
    total_seconds <- ((median(times()$avg_seconds)))
    duration <- seconds_to_period(total_seconds)
    
    string <- paste(
      if (hour(duration) > 0) paste0(hour(duration), " hour", ifelse(hour(duration) == 1, "", "s")) else NULL,
      if (minute(duration) > 0) paste0(minute(duration), " minute", ifelse(minute(duration) == 1, "", "s")) else NULL,
      if (round(second(duration), 0) > 0) paste0(round(second(duration), 0), " second", ifelse(round(second(duration), 0) == 1, "", "s")) else NULL,
      sep = ", "
    )
    
    string <- paste0("Median: ", string)
  })
  
  output$loc_max <-renderText({
    total_seconds <- ((max(times()$avg_seconds)))
    duration <- seconds_to_period(total_seconds)
    string <- paste(
      if (hour(duration) > 0) paste0(hour(duration), " hour", ifelse(hour(duration) == 1, "", "s")) else NULL,
      if (minute(duration) > 0) paste0(minute(duration), " minute", ifelse(minute(duration) == 1, "", "s")) else NULL,
      if (round(second(duration), 0) > 0) paste0(round(second(duration), 0), " second", ifelse(round(second(duration), 0) == 1, "", "s")) else NULL,
      sep = ", "
    )
    
    string <- paste0("Max: ", string)
  })
  
  output$loc_min <-renderText({
    total_seconds <- ((min(times()$avg_seconds)))
    duration <- seconds_to_period(total_seconds)
    string <- paste(
      if (hour(duration) > 0) paste0(hour(duration), " hour", ifelse(hour(duration) == 1, "", "s")) else NULL,
      if (minute(duration) > 0) paste0(minute(duration), " minute", ifelse(minute(duration) == 1, "", "s")) else NULL,
      if (round(second(duration), 0) > 0) paste0(round(second(duration), 0), " second", ifelse(round(second(duration), 0) == 1, "", "s")) else NULL,
      sep = ", "
    )
    
    string <- paste0("Min: ", string)
  })
  
  output$data <- DT::renderDT(rawTimes())
  
  output$timePlot <- plotly::renderPlotly({
    p <- ggplot(times(), aes(x = datetime, y = avg_seconds, color = weekday)) +
      geom_line() +
      geom_point() +
      labs(
        title = "Average Seconds Across the Week (Interactive)",
        x = "Day and Time",
        y = "Average Seconds",
        color = "Weekday"
      ) +
      scale_x_datetime(
        date_labels = "%a %H:%M",
        breaks = scales::date_breaks("12 hours")
      ) +
      theme_minimal()
    
    plotly::ggplotly(p)
  })
}

# Run the application 
shinyApp(ui = ui, server = server)

